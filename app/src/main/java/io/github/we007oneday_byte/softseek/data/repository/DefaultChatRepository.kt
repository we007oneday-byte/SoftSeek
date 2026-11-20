package io.github.we007oneday_byte.softseek.data.repository

import io.github.we007oneday_byte.softseek.core.logging.AppLog
import io.github.we007oneday_byte.softseek.core.logging.LogModule
import io.github.we007oneday_byte.softseek.domain.auth.ApiKeyStore
import io.github.we007oneday_byte.softseek.domain.context.ContextAssembler
import io.github.we007oneday_byte.softseek.domain.model.AiChatRequest
import io.github.we007oneday_byte.softseek.domain.model.AiImage
import io.github.we007oneday_byte.softseek.domain.model.AiMessage
import io.github.we007oneday_byte.softseek.domain.model.AttachmentType
import io.github.we007oneday_byte.softseek.domain.model.ChatError
import io.github.we007oneday_byte.softseek.domain.model.ChatFinishReason
import io.github.we007oneday_byte.softseek.domain.model.ChatMessage
import io.github.we007oneday_byte.softseek.domain.model.ChatSendEvent
import io.github.we007oneday_byte.softseek.domain.model.ChatStreamEvent
import io.github.we007oneday_byte.softseek.domain.model.ContextBuildResult
import io.github.we007oneday_byte.softseek.domain.model.Conversation
import io.github.we007oneday_byte.softseek.domain.model.ImageDetail
import io.github.we007oneday_byte.softseek.domain.model.MessageAttachment
import io.github.we007oneday_byte.softseek.domain.model.MessageRole
import io.github.we007oneday_byte.softseek.domain.model.MessageStatus
import io.github.we007oneday_byte.softseek.domain.model.TokenUsage
import io.github.we007oneday_byte.softseek.domain.model.userMessage
import io.github.we007oneday_byte.softseek.domain.provider.AiProvider
import io.github.we007oneday_byte.softseek.domain.provider.AiProviderException
import io.github.we007oneday_byte.softseek.domain.repository.ChatRepository
import io.github.we007oneday_byte.softseek.domain.repository.ConversationRepository
import io.github.we007oneday_byte.softseek.domain.repository.MemoryPoolRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.Base64
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultChatRepository
    @Inject
    constructor(
        private val provider: AiProvider,
        private val conversationRepository: ConversationRepository,
        private val memoryPoolRepository: MemoryPoolRepository,
        private val contextAssembler: ContextAssembler,
        private val apiKeyStore: ApiKeyStore,
    ) : ChatRepository {
        override fun sendMessage(
            conversationId: String,
            text: String,
            attachments: List<MessageAttachment>,
        ): Flow<ChatSendEvent> = flow {
            val content = text.trim()
            if (content.isEmpty() && attachments.isEmpty()) {
                emit(ChatSendEvent.Failed(null, ChatError.BadRequest("消息不能为空")))
                return@flow
            }
            val apiKey = apiKeyStore.apiKey.value
            if (apiKey.isNullOrBlank()) {
                emit(ChatSendEvent.Failed(null, ChatError.NoApiKey))
                return@flow
            }
            val conversation = conversationRepository.getConversation(conversationId)
            if (conversation == null) {
                emit(ChatSendEvent.Failed(null, ChatError.BadRequest("会话不存在")))
                return@flow
            }
            val existing = conversationRepository.getMessages(conversationId)
            val timestamp = nextTimestamp(existing)
            val imageAttachments = attachments.mapIndexed { index, attachment ->
                attachment.copy(nodeId = "", sortOrder = index)
            }
            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                parentId = existing.lastOrNull()?.id,
                role = MessageRole.USER,
                content = content,
                reasoningContent = null,
                reasoningTokens = 0,
                modelName = conversation.modelName,
                status = MessageStatus.COMPLETE,
                finishReason = "stop",
                errorMessage = null,
                promptTokens = 0,
                completionTokens = 0,
                totalTokens = 0,
                timestamp = timestamp,
                isDeleted = false,
                deletedAt = null,
                attachments = imageAttachments.map { it.copy(nodeId = "") },
            )
            conversationRepository.addMessage(userMessage.copy(attachments = imageAttachments))

            val config = memoryPoolRepository.getConfig(conversation.id)
            val assembled = assembleContext(conversation, conversationRepository.getMessages(conversationId))
            val assistant = newAssistant(conversation, parentId = userMessage.id, timestamp = timestamp + 1)
            conversationRepository.addMessage(assistant)
            emit(ChatSendEvent.Started(userMessage, assistant))
            emitAll(generate(conversation, assistant, apiKey, assembled, config.imageDetail))
        }.flowOn(Dispatchers.IO)

        override fun regenerate(conversationId: String): Flow<ChatSendEvent> = flow {
            val apiKey = apiKeyStore.apiKey.value
            if (apiKey.isNullOrBlank()) {
                emit(ChatSendEvent.Failed(null, ChatError.NoApiKey))
                return@flow
            }
            val conversation = conversationRepository.getConversation(conversationId)
            if (conversation == null) {
                emit(ChatSendEvent.Failed(null, ChatError.BadRequest("会话不存在")))
                return@flow
            }
            val all = conversationRepository.getMessages(conversationId)
            val lastAssistant = all.lastOrNull { it.role == MessageRole.ASSISTANT && !it.isDeleted }
            if (lastAssistant != null) {
                conversationRepository.softDeleteMessage(lastAssistant.id)
            }
            val remaining = conversationRepository.getMessages(conversationId)
            val lastUser = remaining.lastOrNull { it.role == MessageRole.USER && !it.isDeleted }
            if (lastUser == null) {
                emit(ChatSendEvent.Failed(null, ChatError.BadRequest("没有可重新生成的用户消息")))
                return@flow
            }
            val assembled = assembleContext(conversation, remaining)
            val config = memoryPoolRepository.getConfig(conversation.id)
            val assistant = newAssistant(conversation, parentId = lastUser.id, timestamp = nextTimestamp(remaining))
            conversationRepository.addMessage(assistant)
            emit(ChatSendEvent.Started(null, assistant))
            emitAll(generate(conversation, assistant, apiKey, assembled, config.imageDetail))
        }.flowOn(Dispatchers.IO)

        private suspend fun assembleContext(
            conversation: Conversation,
            history: List<ChatMessage>,
        ): ContextBuildResult {
            val config = memoryPoolRepository.getConfig(conversation.id)
            return contextAssembler.assemble(
                systemPrompt = conversation.systemPrompt,
                history = history,
                maxInputTokens = config.maxInputTokens,
                tokenEstimateRatio = config.tokenEstimateRatio,
            )
        }

        private fun newAssistant(
            conversation: Conversation,
            parentId: String?,
            timestamp: Long,
        ): ChatMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversation.id,
            parentId = parentId,
            role = MessageRole.ASSISTANT,
            content = "",
            reasoningContent = null,
            reasoningTokens = 0,
            modelName = conversation.modelName,
            status = MessageStatus.PARTIAL,
            finishReason = null,
            errorMessage = null,
            promptTokens = 0,
            completionTokens = 0,
            totalTokens = 0,
            timestamp = timestamp,
            isDeleted = false,
            deletedAt = null,
            attachments = emptyList(),
        )

        private fun generate(
            conversation: Conversation,
            assistant: ChatMessage,
            apiKey: String,
            assembled: ContextBuildResult,
            imageDetail: ImageDetail,
        ): Flow<ChatSendEvent> = flow {
            val content = StringBuilder()
            val reasoning = StringBuilder()
            var usage = TokenUsage()
            var finishReason: ChatFinishReason? = null
            var lastPersistedAt = 0L
            var lastPersistedLength = 0

            suspend fun persist(
                status: MessageStatus,
                finish: ChatFinishReason?,
                error: ChatError?,
            ): ChatMessage {
                val updated = assistant.copy(
                    content = content.toString(),
                    reasoningContent = reasoning.toString().ifBlank { null },
                    reasoningTokens = usage.reasoningTokens,
                    status = status,
                    finishReason = finish?.name?.lowercase(),
                    errorMessage = error?.userMessage(),
                    promptTokens = usage.promptTokens,
                    completionTokens = usage.completionTokens,
                    totalTokens = usage.totalTokens,
                )
                conversationRepository.updateMessage(updated)
                return updated
            }

            suspend fun persistIfNeeded() {
                val now = System.currentTimeMillis()
                val length = content.length + reasoning.length
                if (now - lastPersistedAt >= PERSIST_INTERVAL_MS || length - lastPersistedLength >= PERSIST_CHARS) {
                    lastPersistedAt = now
                    lastPersistedLength = length
                    persist(MessageStatus.PARTIAL, finishReason, null)
                }
            }

            var failure: ChatError? = null
            try {
                val messages = assembled.messages.map { context ->
                    AiMessage(
                        role = context.role,
                        content = context.content,
                        images = if (context.role == MessageRole.USER) {
                            context.images.mapNotNull { encodeImage(it, imageDetail) }
                        } else {
                            emptyList()
                        },
                    )
                }
                AppLog.info(
                    module = LogModule.CHAT,
                    message = "请求模型 model=${conversation.modelName}",
                )
                provider.streamChat(
                    apiKey = apiKey,
                    request = AiChatRequest(
                        model = conversation.modelName,
                        messages = messages,
                        reasoningEffort = conversation.reasoningEffort,
                        maxOutputTokens = conversation.maxOutputTokens,
                        imageDetail = imageDetail,
                    ),
                ).collect { event ->
                    if (failure != null) return@collect
                    when (event) {
                        is ChatStreamEvent.ContentDelta -> {
                            content.append(event.text)
                            persistIfNeeded()
                        }
                        is ChatStreamEvent.ReasoningDelta -> {
                            reasoning.append(event.text)
                            persistIfNeeded()
                        }
                        is ChatStreamEvent.UsageReceived -> usage = event.usage
                        is ChatStreamEvent.Finished -> finishReason = event.reason
                        is ChatStreamEvent.Failed -> failure = event.error
                        ChatStreamEvent.Completed -> Unit
                    }
                }
            } catch (cancelled: CancellationException) {
                withContext(NonCancellable) {
                    if (content.isNotEmpty() || reasoning.isNotEmpty()) {
                        persist(MessageStatus.PARTIAL, finishReason, null)
                    } else {
                        conversationRepository.softDeleteMessage(assistant.id)
                    }
                }
                throw cancelled
            } catch (error: AiProviderException) {
                failure = error.error
            } catch (error: IOException) {
                failure = ChatError.Network(error.message.orEmpty())
            } catch (error: Exception) {
                failure = ChatError.Protocol(error.message.orEmpty())
            }

            val error = failure
            if (error != null) {
                val failed = persist(MessageStatus.FAILED, finishReason, error)
                emit(ChatSendEvent.Failed(failed, error))
            } else {
                if (content.isEmpty() && reasoning.isEmpty()) {
                    val emptyError = ChatError.Protocol("流式响应为空")
                    val failed = persist(MessageStatus.FAILED, finishReason, emptyError)
                    emit(ChatSendEvent.Failed(failed, emptyError))
                } else {
                    val completed = persist(MessageStatus.COMPLETE, finishReason ?: ChatFinishReason.STOP, null)
                    emit(ChatSendEvent.Finished(completed))
                }
            }
        }.flowOn(Dispatchers.IO)

        private suspend fun encodeImage(
            attachment: MessageAttachment,
            detail: ImageDetail,
        ): AiImage? = withContext(Dispatchers.IO) {
            if (attachment.type != AttachmentType.IMAGE) return@withContext null
            runCatching {
                val bytes = File(attachment.localPath).readBytes()
                require(bytes.size <= MAX_IMAGE_BYTES) { "图片过大" }
                AiImage(
                    mime = attachment.mime.ifBlank { "image/jpeg" },
                    base64 = Base64.getEncoder().encodeToString(bytes),
                    detail = detail,
                )
            }.getOrNull()
        }

        private fun nextTimestamp(messages: List<ChatMessage>): Long {
            val now = System.currentTimeMillis()
            val last = messages.maxOfOrNull { it.timestamp } ?: 0L
            return maxOf(now, last + 1)
        }

        private companion object {
            const val PERSIST_INTERVAL_MS = 250L
            const val PERSIST_CHARS = 200
            const val MAX_IMAGE_BYTES = 6L * 1024 * 1024
        }
    }
