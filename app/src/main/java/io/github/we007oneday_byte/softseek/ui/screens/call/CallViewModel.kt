package io.github.we007oneday_byte.softseek.ui.screens.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.we007oneday_byte.softseek.core.asr.AsrEngine
import io.github.we007oneday_byte.softseek.core.asr.AsrEngineFactory
import io.github.we007oneday_byte.softseek.core.asr.AsrEvent
import io.github.we007oneday_byte.softseek.core.audio.AudioCapture
import io.github.we007oneday_byte.softseek.core.tts.TtsEngine
import io.github.we007oneday_byte.softseek.core.tts.TtsEvent
import io.github.we007oneday_byte.softseek.data.settings.SettingsRepository
import io.github.we007oneday_byte.softseek.domain.model.ChatSendEvent
import io.github.we007oneday_byte.softseek.domain.model.MemoryPoolConfig
import io.github.we007oneday_byte.softseek.domain.model.userMessage
import io.github.we007oneday_byte.softseek.domain.repository.ChatRepository
import io.github.we007oneday_byte.softseek.domain.repository.ConversationRepository
import io.github.we007oneday_byte.softseek.domain.repository.MemoryPoolRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class CallPhase {
    DIALING,
    LISTENING,
    THINKING,
    SPEAKING,
    ENDED,
    ERROR,
}

data class CallUiState(
    val phase: CallPhase = CallPhase.DIALING,
    val partialText: String = "",
    val lastUserText: String = "",
    val assistantText: String = "",
    val error: String? = null,
    val durationSeconds: Int = 0,
    val muted: Boolean = false,
    val inputText: String = "",
    val showKeyboard: Boolean = false,
)

/**
 * 半双工通话状态机：Listening -> Thinking -> Speaking -> Listening。
 * 录音和播报不同时开，首版优先保证无回声；ASR 的端点检测负责进入 Thinking。
 */
@HiltViewModel
class CallViewModel
    @Inject
    constructor(
        private val asrEngineFactory: AsrEngineFactory,
        private val ttsEngine: TtsEngine,
        private val chatRepository: ChatRepository,
        private val conversationRepository: ConversationRepository,
        private val memoryPoolRepository: MemoryPoolRepository,
        private val settingsRepository: SettingsRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CallUiState())
        val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

        private val capture = AudioCapture()
        private var asrEngine: AsrEngine? = null
        private var conversationId: String? = null
        private var audioJob: Job? = null
        private var timerJob: Job? = null
        private var sendJob: Job? = null
        private var ttsWaiter: CompletableDeferred<Boolean>? = null
        private var started = false
        private var ended = false
        private var busy = false

        init {
            ttsEngine.events
                .onEach { event ->
                    when (event) {
                        is TtsEvent.Done -> ttsWaiter?.complete(true)
                        is TtsEvent.Error -> {
                            ttsWaiter?.complete(false)
                            _uiState.update { it.copy(error = event.message) }
                        }
                        is TtsEvent.Started -> Unit
                    }
                }
                .launchIn(viewModelScope)
        }

        fun startCall() {
            if (started) return
            started = true
            viewModelScope.launch {
                try {
                    val settings = settingsRepository.settings.value
                    val conversation = conversationRepository.createConversation(
                        title = callTitle(),
                        modelName = settings.defaultModel,
                        reasoningEffort = settings.defaultReasoningEffort,
                        maxOutputTokens = settings.defaultMaxOutputTokens,
                    )
                    conversationId = conversation.id
                    memoryPoolRepository.upsertConfig(
                        MemoryPoolConfig.defaults(conversation.id, System.currentTimeMillis()).copy(
                            maxInputTokens = settings.defaultMaxInputTokens,
                            enableAutoCleanup = settings.autoCleanup,
                            cleanupThresholdCount = settings.cleanupThresholdCount,
                            cleanupProtectionHours = settings.cleanupProtectionHours,
                            deletedRetentionDays = settings.deletedRetentionDays,
                            imageDetail = settings.imageDetail,
                        ),
                    )

                    val engine = asrEngineFactory.create().getOrElse { throw it }
                    asrEngine = engine
                    engine.events
                        .onEach { event ->
                            when (event) {
                                is AsrEvent.Partial -> {
                                    if (!ended) _uiState.update { it.copy(partialText = event.text) }
                                }
                                is AsrEvent.Final -> {
                                    if (!ended) handleUserText(event.text)
                                }
                                is AsrEvent.Error -> {
                                    if (!ended) _uiState.update { it.copy(error = event.message) }
                                }
                            }
                        }
                        .launchIn(viewModelScope)

                    engine.start().getOrElse { throw it }
                    capture.start().getOrElse { throw it }
                    _uiState.update {
                        it.copy(
                            phase = CallPhase.LISTENING,
                            error = null,
                            partialText = "",
                        )
                    }
                    startAudioLoop()
                    startTimer()
                } catch (error: Exception) {
                    _uiState.update {
                        it.copy(
                            phase = CallPhase.ERROR,
                            error = error.message ?: "通话初始化失败",
                        )
                    }
                }
            }
        }

        fun onInputChange(value: String) {
            _uiState.update { it.copy(inputText = value) }
        }

        fun toggleKeyboard() {
            _uiState.update { it.copy(showKeyboard = !it.showKeyboard) }
        }

        fun sendText() {
            val text = _uiState.value.inputText.trim()
            if (text.isEmpty()) return
            _uiState.update { it.copy(inputText = "", showKeyboard = false) }
            handleUserText(text)
        }

        fun toggleMute() {
            val muted = !_uiState.value.muted
            _uiState.update { it.copy(muted = muted) }
            if (muted) {
                stopCapture()
            } else if (!ended && !busy) {
                startListening()
            }
        }

        fun endCall() {
            if (ended) return
            ended = true
            busy = false
            sendJob?.cancel()
            sendJob = null
            stopCapture()
            timerJob?.cancel()
            timerJob = null
            ttsWaiter?.complete(false)
            ttsWaiter = null
            ttsEngine.stop()
            asrEngine?.let {
                runCatching { it.finish() }
                runCatching { it.release() }
            }
            asrEngine = null
            _uiState.update {
                it.copy(
                    phase = CallPhase.ENDED,
                    partialText = "",
                    error = null,
                )
            }
        }

        override fun onCleared() {
            endCall()
            ttsEngine.stop()
            super.onCleared()
        }

        private fun startAudioLoop() {
            audioJob?.cancel()
            audioJob = viewModelScope.launch(Dispatchers.IO) {
                val buffer = ShortArray(AUDIO_FRAME_SAMPLES)
                while (true) {
                    val read = capture.read(buffer)
                    if (read <= 0) {
                        if (!ended) delay(10)
                        continue
                    }
                    val engine = asrEngine ?: break
                    val frame = if (read == buffer.size) buffer else buffer.copyOf(read)
                    engine.acceptAudio(frame, 0, frame.size)
                }
            }
        }

        private fun stopCapture() {
            audioJob?.cancel()
            audioJob = null
            capture.stop()
        }

        private fun startListening() {
            if (ended || busy || _uiState.value.muted) return
            capture.start()
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            phase = CallPhase.LISTENING,
                            partialText = "",
                            error = null,
                        )
                    }
                    startAudioLoop()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            phase = CallPhase.ERROR,
                            error = error.message ?: "麦克风启动失败",
                        )
                    }
                }
        }

        private fun handleUserText(raw: String) {
            val text = raw.trim()
            if (text.isEmpty() || busy || ended) return
            busy = true
            stopCapture()
            _uiState.update {
                it.copy(
                    phase = CallPhase.THINKING,
                    lastUserText = text,
                    partialText = "",
                    assistantText = "",
                    error = null,
                    inputText = "",
                    showKeyboard = false,
                )
            }
            sendJob = viewModelScope.launch {
                var finalText = ""
                var failure: String? = null
                try {
                    val id = conversationId ?: throw IllegalStateException("通话会话尚未创建")
                    chatRepository.sendMessage(id, text).collect { event ->
                        when (event) {
                            is ChatSendEvent.Started -> Unit
                            is ChatSendEvent.Delta -> {
                                finalText = event.content
                                _uiState.update { it.copy(assistantText = event.content) }
                            }
                            is ChatSendEvent.Finished -> {
                                finalText = event.assistantMessage.content
                                _uiState.update { it.copy(assistantText = finalText) }
                            }
                            is ChatSendEvent.Failed -> {
                                failure = event.error.userMessage()
                            }
                            is ChatSendEvent.Stopped -> Unit
                        }
                    }
                    if (failure != null) {
                        _uiState.update {
                            it.copy(
                                phase = CallPhase.ERROR,
                                error = failure,
                            )
                        }
                    } else {
                        speakAndWait(finalText)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    _uiState.update {
                        it.copy(
                            phase = CallPhase.ERROR,
                            error = error.message ?: "生成回复失败",
                        )
                    }
                } finally {
                    busy = false
                    if (!ended && _uiState.value.phase != CallPhase.ERROR) {
                        startListening()
                    }
                }
            }
        }

        private suspend fun speakAndWait(text: String) {
            if (text.isBlank() || ended) return
            _uiState.update { it.copy(phase = CallPhase.SPEAKING) }
            val waiter = CompletableDeferred<Boolean>()
            ttsWaiter = waiter
            val result = ttsEngine.speak(text, "call-${System.currentTimeMillis()}")
            if (result.isFailure) {
                ttsWaiter = null
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "系统 TTS 不可用")
                }
                return
            }
            val completed = withTimeoutOrNull(TTS_TIMEOUT_MILLIS) { waiter.await() }
            ttsWaiter = null
            if (completed == null) {
                ttsEngine.stop()
                _uiState.update { it.copy(error = "语音播报超时") }
            }
        }

        private fun startTimer() {
            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                while (!ended) {
                    delay(1_000)
                    if (!ended) {
                        _uiState.update { it.copy(durationSeconds = it.durationSeconds + 1) }
                    }
                }
            }
        }

        private fun callTitle(): String =
            "语音通话 ${SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date())}"

        private companion object {
            const val AUDIO_FRAME_SAMPLES = 1_024
            const val TTS_TIMEOUT_MILLIS = 90_000L
        }
    }
