package io.github.we007oneday_byte.softseek.core.tts

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 系统 TTS 封装：只使用设备已安装的语音引擎，不下载第三方声音。
 * 初始化失败、缺少中文语音或单句播报失败都会通过 [events] 暴露，调用方不得静默失败。
 */
@Singleton
class SystemTtsEngine
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : TtsEngine {
        private val appContext = context.applicationContext
        private val mainHandler = Handler(Looper.getMainLooper())
        private val _events = MutableSharedFlow<TtsEvent>(
            extraBufferCapacity = 16,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
        override val events: SharedFlow<TtsEvent> = _events.asSharedFlow()
        private val _ready = MutableStateFlow(false)
        override val ready: StateFlow<Boolean> = _ready.asStateFlow()

        private var tts: TextToSpeech? = null
        private var released = false

        init {
            mainHandler.post {
                if (released) return@post
                tts = TextToSpeech(appContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        configureLanguage()
                    } else {
                        _ready.value = false
                        _events.tryEmit(TtsEvent.Error(null, "系统 TTS 初始化失败"))
                    }
                }
            }
        }

        override fun speak(
            text: String,
            utteranceId: String,
        ): Result<Unit> {
            if (text.isBlank()) return Result.failure(IllegalArgumentException("播报文本为空"))
            val engine = tts ?: return Result.failure(IllegalStateException("系统 TTS 尚未初始化"))
            if (!_ready.value) return Result.failure(IllegalStateException("系统 TTS 未就绪"))
            val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            return if (result == TextToSpeech.SUCCESS) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("系统 TTS 播报失败"))
            }
        }

        override fun stop() {
            tts?.stop()
        }

        override fun setRate(rate: Float) {
            tts?.setSpeechRate(rate.coerceIn(MIN_RATE, MAX_RATE))
        }

        override fun setPitch(pitch: Float) {
            tts?.setPitch(pitch.coerceIn(MIN_PITCH, MAX_PITCH))
        }

        override fun release() {
            released = true
            _ready.value = false
            tts?.stop()
            tts?.shutdown()
            tts = null
        }

        private fun configureLanguage() {
            val engine = tts ?: return
            engine.setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        utteranceId?.let { _events.tryEmit(TtsEvent.Started(it)) }
                    }

                    override fun onDone(utteranceId: String?) {
                        utteranceId?.let { _events.tryEmit(TtsEvent.Done(it)) }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _events.tryEmit(TtsEvent.Error(utteranceId, "TTS 播报失败"))
                    }

                    override fun onError(
                        utteranceId: String?,
                        errorCode: Int,
                    ) {
                        _events.tryEmit(TtsEvent.Error(utteranceId, "TTS 错误码 $errorCode"))
                    }
                },
            )

            val candidates = listOf(Locale.SIMPLIFIED_CHINESE, Locale.CHINA, Locale.CHINESE)
            val available = candidates.firstOrNull { locale ->
                val result = engine.setLanguage(locale)
                result == TextToSpeech.LANG_AVAILABLE ||
                    result == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                    result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
            }
            if (available == null) {
                _ready.value = false
                _events.tryEmit(TtsEvent.Error(null, "设备未安装中文系统 TTS 语音"))
            } else {
                _ready.value = true
            }
        }

        companion object {
            private const val MIN_RATE = 0.5f
            private const val MAX_RATE = 2.0f
            private const val MIN_PITCH = 0.5f
            private const val MAX_PITCH = 2.0f
        }
    }
