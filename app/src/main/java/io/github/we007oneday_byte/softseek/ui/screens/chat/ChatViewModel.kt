package io.github.we007oneday_byte.softseek.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.we007oneday_byte.softseek.core.constant.AppConstants
import io.github.we007oneday_byte.softseek.domain.auth.ApiKeyStore
import io.github.we007oneday_byte.softseek.domain.model.ChatMessage
import io.github.we007oneday_byte.softseek.domain.model.ChatSendEvent
import io.github.we007oneday_byte.softseek.domain.model.Conversation
import io.github.we007oneday_byte.softseek.domain.model.MessageRole
import io.github.we007oneday_byte.softseek.domain.model.ReasoningEffort
import io.github.we007oneday_byte.softseek.domain.model.userMessage
import io.github.we007oneday_byte.softseek.domain.repository.ChatRepository
import io.github.we007oneday_byte.softseek.domain.repository.ConversationRepository
import kotlinx.coroutines.CancellationException
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
)

@HiltViewModel
class ChatViewModel
    @Inject
    constructor(
        private val conversationRepository: ConversationRepository,
        private val chatRepository: ChatRepository,
        apiKeyStore: ApiKeyStore,
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
        }

        fun onInputChange(value: String) {
            _uiState.update { it.copy(input = value) }
        }

        fun clearError() {
            _uiState.update { it.copy(errorMessage = null) }
        }

        fun send() {
            val text = _uiState.value.input.trim()
            if (text.isEmpty() || _uiState.value.isStreaming) return
            generation += 1
            val runGeneration = generation
            _uiState.update {
                it.copy(
                    input = "",
                    errorMessage = null,
                    streamingAssistantId = null,
                    streamingContent = "",
                    streamingReasoning = "",
                )
            }
            sendJob = viewModelScope.launch {
                val conversationId = activeId.value ?: createConversation(text)
                if (conversationId == null || runGeneration != generation) return@launch
                collectSend(chatRepository.sendMessage(conversationId, text), runGeneration)
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
                collectSend(chatRepository.regenerate(conversationId), runGeneration)
            }
        }

        private suspend fun createConversation(firstMessage: String): String? {
            val title = firstMessage.lineSequence().firstOrNull()?.trim()?.take(MAX_TITLE_LENGTH)
            if (title.isNullOrBlank()) return null
            val conversation = conversationRepository.createConversation(
                title = title,
                modelName = AppConstants.DEFAULT_MODEL,
                reasoningEffort = ReasoningEffort.HIGH,
                maxOutputTokens = DEFAULT_MAX_OUTPUT_TOKENS,
            )
            activeId.value = conversation.id
            _uiState.update {
                it.copy(
                    activeConversationId = conversation.id,
                    activeConversation = conversation,
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
            const val DEFAULT_MAX_OUTPUT_TOKENS = 8192
        }
    }
