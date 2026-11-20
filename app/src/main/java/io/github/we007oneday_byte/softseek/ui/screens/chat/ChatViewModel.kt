package io.github.we007oneday_byte.softseek.ui.screens.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.we007oneday_byte.softseek.core.image.ImageProcessor
import io.github.we007oneday_byte.softseek.core.logging.AppLog
import io.github.we007oneday_byte.softseek.core.logging.LogModule
import io.github.we007oneday_byte.softseek.data.model.ModelCatalog
import io.github.we007oneday_byte.softseek.data.settings.SettingsRepository
import io.github.we007oneday_byte.softseek.domain.auth.ApiKeyStore
import io.github.we007oneday_byte.softseek.domain.model.AiModelInfo
import io.github.we007oneday_byte.softseek.domain.model.ChatMessage
import io.github.we007oneday_byte.softseek.domain.model.ChatSendEvent
import io.github.we007oneday_byte.softseek.domain.model.Conversation
import io.github.we007oneday_byte.softseek.domain.model.MemoryPoolConfig
import io.github.we007oneday_byte.softseek.domain.model.MessageAttachment
import io.github.we007oneday_byte.softseek.domain.model.MessageRole
import io.github.we007oneday_byte.softseek.domain.model.ReasoningEffort
import io.github.we007oneday_byte.softseek.domain.model.userMessage
import io.github.we007oneday_byte.softseek.domain.repository.ChatRepository
import io.github.we007oneday_byte.softseek.domain.repository.ConversationRepository
import io.github.we007oneday_byte.softseek.domain.repository.MemoryPoolRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val conversations: List<Conversation> = emptyList(),
    val activeConversationId: String? = null,
    val activeConversation: Conversation? = null,
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isStreaming: Boolean = false,
    val streamingAssistantId: String? = null,
    val streamingContent: String = "",
    val streamingReasoning: String = "",
    val errorMessage: String? = null,
    val apiKeyConfigured: Boolean = false,
    val models: List<AiModelInfo> = emptyList(),
    val currentModelInfo: AiModelInfo? = null,
    val pendingImages: List<MessageAttachment> = emptyList(),
    val isProcessingImages: Boolean = false,
    val visionEnabled: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel
    @Inject
    constructor(
        private val conversationRepository: ConversationRepository,
        private val chatRepository: ChatRepository,
        private val memoryPoolRepository: MemoryPoolRepository,
        private val settingsRepository: SettingsRepository,
        private val modelCatalog: ModelCatalog,
        private val imageProcessor: ImageProcessor,
        private val apiKeyStore: ApiKeyStore,
    ) : ViewModel() {
        private val activeId = MutableStateFlow<String?>(null)
        private val _uiState = MutableStateFlow(ChatUiState())
        val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

        private var sendJob: Job? = null
        private var generation = 0

        init {
            conversationRepository.observeConversations()
                .onEach { conversations ->
                    _uiState.update { state ->
                        val active = conversations.firstOrNull { it.id == activeId.value }
                        state.copy(
                            conversations = conversations,
                            activeConversation = active,
                            currentModelInfo = active?.let { modelCatalog.model(it.modelName) },
                            visionEnabled = active?.let { modelCatalog.supportsVision(it.modelName) } ?: false,
                        )
                    }
                }
                .launchIn(viewModelScope)

            modelCatalog.models
                .onEach { models ->
                    _uiState.update { state ->
                        state.copy(
                            models = models,
                            currentModelInfo = state.activeConversation?.let { modelCatalog.model(it.modelName) }
                                ?: models.firstOrNull(),
                            visionEnabled = state.activeConversation?.let { modelCatalog.supportsVision(it.modelName) }
                                ?: (models.firstOrNull()?.inputModalities?.contains("image") == true),
                        )
                    }
                }
                .launchIn(viewModelScope)

            activeId
                .flatMapLatest { id ->
                    if (id == null) flowOf(emptyList()) else conversationRepository.observeMessages(id)
                }
                .onEach { messages -> _uiState.update { it.copy(messages = messages) } }
                .launchIn(viewModelScope)

            apiKeyStore.apiKey
                .onEach { key -> _uiState.update { it.copy(apiKeyConfigured = !key.isNullOrBlank()) } }
                .launchIn(viewModelScope)

            settingsRepository.settings
                .onEach { settings ->
                    settings.manualModelIds.forEach { modelCatalog.ensureModel(it) }
                }
                .launchIn(viewModelScope)

            viewModelScope.launch {
                val key = apiKeyStore.apiKey.value
                if (!key.isNullOrBlank()) {
                    modelCatalog.refresh(key)
                    reconcileModels()
                }
            }
        }

        fun onInputChange(value: String) {
            _uiState.update { it.copy(input = value) }
        }

        fun clearError() {
            _uiState.update { it.copy(errorMessage = null) }
        }

        fun addImages(uris: List<Uri>) {
            if (uris.isEmpty() || _uiState.value.isProcessingImages) return
            val existing = _uiState.value.pendingImages.size
            if (existing >= MAX_IMAGES) return
            val accepted = uris.take(MAX_IMAGES - existing)
            viewModelScope.launch {
                _uiState.update { it.copy(isProcessingImages = true) }
                runCatching { imageProcessor.prepare(accepted, existing) }
                    .onSuccess { prepared ->
                        _uiState.update {
                            it.copy(
                                pendingImages = (it.pendingImages + prepared).take(MAX_IMAGES),
                                isProcessingImages = false,
                            )
                        }
                    }
                    .onFailure { error ->
                        AppLog.error(LogModule.CHAT, "图片处理失败", error)
                        _uiState.update {
                            it.copy(
                                isProcessingImages = false,
                                errorMessage = error.message ?: "图片处理失败",
                            )
                        }
                    }
            }
        }

        fun removePendingImage(attachmentId: String) {
            _uiState.update { state ->
                state.copy(pendingImages = state.pendingImages.filterNot { it.attachmentId == attachmentId })
            }
        }

        fun refreshModels() {
            viewModelScope.launch {
                val key = apiKeyStore.apiKey.value
                if (key.isNullOrBlank()) return@launch
                val result = modelCatalog.refresh(key)
                if (result.isSuccess) reconcileModels()
            }
        }

        /**
         * 远程模型目录变化后，把已保存的默认模型和当前会话模型迁移到实际可用的模型，
         * 避免切换服务商后旧会话仍请求旧模型而返回 Model Not exist。
         */
        private suspend fun reconcileModels() {
            val models = modelCatalog.models.value
            if (models.isEmpty()) return
            val settings = settingsRepository.settings.value
            val fallback = models.first().id
            if (!isKnownModel(settings.defaultModel)) {
                settingsRepository.update { it.copy(defaultModel = fallback) }
            }
            val active = _uiState.value.activeConversation ?: return
            if (!isKnownModel(active.modelName)) {
                conversationRepository.updateConfig(
                    active.id,
                    fallback,
                    active.reasoningEffort,
                    active.maxOutputTokens,
                )
            }
        }

        private fun isKnownModel(id: String): Boolean =
            modelCatalog.contains(id) || settingsRepository.settings.value.manualModelIds.contains(id)

        fun updateConversationConfig(
            model: AiModelInfo,
            effort: ReasoningEffort,
            maxOutputTokens: Int,
        ) {
            val modelId = model.id
            val wasKnown = isKnownModel(modelId)
            val resolved = modelCatalog.ensureModel(model)
            val max = maxOutputTokens.coerceIn(
                MIN_OUTPUT_TOKENS,
                resolved.maxOutputTokens.takeIf { it > 0 } ?: MAX_OUTPUT_TOKENS,
            )
            val active = _uiState.value.activeConversation
            if (active == null) {
                viewModelScope.launch {
                    settingsRepository.update {
                        it.copy(
                            defaultModel = modelId,
                            defaultReasoningEffort = effort,
                            defaultMaxOutputTokens = max,
                            manualModelIds = if (wasKnown) {
                                it.manualModelIds
                            } else {
                                (it.manualModelIds + modelId).distinct().takeLast(MAX_MANUAL_MODELS)
                            },
                        )
                    }
                }
            } else {
                viewModelScope.launch {
                    conversationRepository.updateConfig(active.id, modelId, effort, max)
                    if (!wasKnown) {
                        settingsRepository.update {
                            it.copy(
                                manualModelIds = (it.manualModelIds + modelId)
                                    .distinct()
                                    .takeLast(MAX_MANUAL_MODELS),
                            )
                        }
                    }
                }
            }
            _uiState.update {
                it.copy(
                    currentModelInfo = resolved,
                    visionEnabled = modelCatalog.supportsVision(modelId),
                    activeConversation = it.activeConversation?.copy(
                        modelName = modelId,
                        reasoningEffort = effort,
                        maxOutputTokens = max,
                    ),
                )
            }
        }

        fun send() {
            val text = _uiState.value.input.trim()
            val images = _uiState.value.pendingImages
            if ((text.isEmpty() && images.isEmpty()) || _uiState.value.isStreaming) return
            if (images.isNotEmpty() && !_uiState.value.visionEnabled) {
                _uiState.update { it.copy(errorMessage = "当前模型不支持图片输入，请切换支持图片的模型") }
                return
            }
            generation += 1
            val runGeneration = generation
            val attached = images
            _uiState.update {
                it.copy(
                    input = "",
                    pendingImages = emptyList(),
                    errorMessage = null,
                    streamingAssistantId = null,
                    streamingContent = "",
                    streamingReasoning = "",
                )
            }
            sendJob = viewModelScope.launch {
                val conversationId = activeId.value ?: createConversation(text.ifBlank { "图片对话" })
                if (conversationId == null || runGeneration != generation) return@launch
                ensureConversationModel(conversationId)
                collectSend(chatRepository.sendMessage(conversationId, text, attached), runGeneration)
            }
        }

        fun regenerate() {
            val conversationId = activeId.value ?: return
            if (_uiState.value.isStreaming) return
            generation += 1
            val runGeneration = generation
            _uiState.update {
                it.copy(
                    errorMessage = null,
                    streamingAssistantId = null,
                    streamingContent = "",
                    streamingReasoning = "",
                )
            }
            sendJob = viewModelScope.launch {
                ensureConversationModel(conversationId)
                collectSend(chatRepository.regenerate(conversationId), runGeneration)
            }
        }

        fun stop() {
            generation += 1
            val stoppedGeneration = generation
            sendJob?.cancel()
            sendJob = null
            _uiState.update { it.copy(isStreaming = false) }
            viewModelScope.launch {
                delay(400)
                if (stoppedGeneration == generation) {
                    _uiState.update {
                        it.copy(
                            streamingAssistantId = null,
                            streamingContent = "",
                            streamingReasoning = "",
                        )
                    }
                }
            }
        }

        fun newChat() {
            stop()
            activeId.value = null
            _uiState.update {
                it.copy(
                    activeConversationId = null,
                    activeConversation = null,
                    messages = emptyList(),
                    input = "",
                    errorMessage = null,
                    streamingAssistantId = null,
                    streamingContent = "",
                    streamingReasoning = "",
                )
            }
        }

        fun selectConversation(conversationId: String) {
            if (conversationId == activeId.value) return
            stop()
            activeId.value = conversationId
            _uiState.update {
                it.copy(
                    activeConversationId = conversationId,
                    activeConversation = it.conversations.firstOrNull { conversation ->
                        conversation.id == conversationId
                    },
                    errorMessage = null,
                )
            }
        }

        fun renameConversation(conversationId: String, title: String) {
            val clean = title.trim()
            if (clean.isEmpty()) return
            viewModelScope.launch {
                conversationRepository.renameConversation(conversationId, clean.take(MAX_TITLE_LENGTH))
            }
        }

        fun togglePinned(conversationId: String) {
            val conversation = _uiState.value.conversations.firstOrNull { it.id == conversationId } ?: return
            viewModelScope.launch {
                conversationRepository.setPinned(conversationId, !conversation.isPinned)
            }
        }

        fun deleteConversation(conversationId: String) {
            if (conversationId == activeId.value) newChat()
            viewModelScope.launch {
                conversationRepository.softDeleteConversation(conversationId)
            }
        }

        fun deleteMessage(messageId: String) {
            viewModelScope.launch {
                conversationRepository.softDeleteMessage(messageId)
            }
        }

        fun editMessage(messageId: String, newContent: String) {
            val content = newContent.trim()
            if (content.isEmpty()) return
            val conversationId = activeId.value ?: return
            generation += 1
            val runGeneration = generation
            sendJob = viewModelScope.launch {
                val messages = _uiState.value.messages
                val target = messages.firstOrNull { it.id == messageId && it.role == MessageRole.USER } ?: return@launch
                conversationRepository.updateMessage(target.copy(content = content))
                messages.filter { it.timestamp > target.timestamp }.forEach { later ->
                    conversationRepository.softDeleteMessage(later.id)
                }
                if (runGeneration != generation) return@launch
                ensureConversationModel(conversationId)
                collectSend(chatRepository.regenerate(conversationId), runGeneration)
            }
        }

        /**
         * 发送、重新生成、编辑重生成前统一检查会话模型；旧版本保存的模型或切换服务商后
         * 已失效的模型会迁移到当前默认/首个可用模型，避免服务端返回 Model Not exist。
         * 用户手动填写并持久化的模型 ID 不会被迁移。
         */
        private suspend fun ensureConversationModel(conversationId: String) {
            val conversation = conversationRepository.getConversation(conversationId) ?: return
            if (isKnownModel(conversation.modelName)) return
            val fallback = settingsRepository.settings.value.defaultModel
                .takeIf { isKnownModel(it) }
                ?: modelCatalog.models.value.firstOrNull()?.id
                ?: return
            conversationRepository.updateConfig(
                conversationId,
                fallback,
                conversation.reasoningEffort,
                conversation.maxOutputTokens,
            )
        }

        private suspend fun createConversation(firstMessage: String): String? {
            val title = firstMessage.lineSequence().firstOrNull()?.trim()?.take(MAX_TITLE_LENGTH)
            if (title.isNullOrBlank()) return null
            val settings = settingsRepository.settings.value
            val fallback = modelCatalog.models.value.firstOrNull()?.id
            val modelId = settings.defaultModel.takeIf { isKnownModel(it) }
                ?: fallback
                ?: settings.defaultModel
            val model = modelCatalog.ensureModel(modelId, settings.defaultMaxOutputTokens)
            val conversation = conversationRepository.createConversation(
                title = title,
                modelName = model.id,
                reasoningEffort = settings.defaultReasoningEffort,
                maxOutputTokens = settings.defaultMaxOutputTokens,
            )
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
            activeId.value = conversation.id
            _uiState.update {
                it.copy(
                    activeConversationId = conversation.id,
                    activeConversation = conversation,
                    currentModelInfo = modelCatalog.model(conversation.modelName),
                    visionEnabled = modelCatalog.supportsVision(conversation.modelName),
                )
            }
            return conversation.id
        }

        private suspend fun collectSend(
            events: kotlinx.coroutines.flow.Flow<ChatSendEvent>,
            runGeneration: Int,
        ) {
            try {
                events.collect { event ->
                    if (runGeneration != generation) return@collect
                    when (event) {
                        is ChatSendEvent.Started -> {
                            _uiState.update { state ->
                                val messages = if (state.messages.any { it.id == event.assistantMessage.id }) {
                                    state.messages
                                } else {
                                    state.messages + event.assistantMessage
                                }
                                state.copy(
                                    messages = messages,
                                    isStreaming = true,
                                    streamingAssistantId = event.assistantMessage.id,
                                    streamingContent = "",
                                    streamingReasoning = "",
                                    errorMessage = null,
                                )
                            }
                        }
                        is ChatSendEvent.Delta -> {
                            _uiState.update {
                                it.copy(
                                    streamingAssistantId = event.assistantMessageId,
                                    streamingContent = event.content,
                                    streamingReasoning = event.reasoning,
                                )
                            }
                        }
                        is ChatSendEvent.Finished -> {
                            _uiState.update { state ->
                                state.copy(
                                    messages = state.messages.replace(event.assistantMessage),
                                    isStreaming = false,
                                    streamingAssistantId = null,
                                    streamingContent = "",
                                    streamingReasoning = "",
                                )
                            }
                        }
                        is ChatSendEvent.Failed -> {
                            val error = event.error.userMessage()
                            AppLog.error(
                                module = LogModule.CHAT,
                                message = "聊天请求失败: $error",
                                metric = "request_fail",
                                value = 1,
                                unit = "次",
                            )
                            _uiState.update { state ->
                                val messages = if (event.assistantMessage != null) {
                                    state.messages.replace(event.assistantMessage)
                                } else {
                                    state.messages
                                }
                                state.copy(
                                    messages = messages,
                                    isStreaming = false,
                                    streamingAssistantId = null,
                                    streamingContent = "",
                                    streamingReasoning = "",
                                    errorMessage = error,
                                )
                            }
                        }
                        is ChatSendEvent.Stopped -> {
                            _uiState.update { it.copy(isStreaming = false) }
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                AppLog.error(LogModule.CHAT, "发送消息异常", error)
                if (runGeneration == generation) {
                    _uiState.update {
                        it.copy(
                            isStreaming = false,
                            errorMessage = error.message.orEmpty().ifBlank { "发送失败" },
                        )
                    }
                }
            } finally {
                if (runGeneration == generation && _uiState.value.isStreaming) {
                    _uiState.update { it.copy(isStreaming = false) }
                }
            }
        }

        private fun List<ChatMessage>.replace(message: ChatMessage): List<ChatMessage> =
            map { if (it.id == message.id) message else it }

        private companion object {
            const val MAX_TITLE_LENGTH = 24
            const val MIN_OUTPUT_TOKENS = 256
            const val MAX_OUTPUT_TOKENS = 393_216
            const val MAX_IMAGES = 8
            const val MAX_MANUAL_MODELS = 30
        }
    }
