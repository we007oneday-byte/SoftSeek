package io.github.we007oneday_byte.softseek.data.repository

import io.github.we007oneday_byte.softseek.domain.auth.ApiKeyStore
import io.github.we007oneday_byte.softseek.domain.auth.ApiKeySummary
import io.github.we007oneday_byte.softseek.domain.context.ContextAssembler
import io.github.we007oneday_byte.softseek.domain.context.TokenEstimator
import io.github.we007oneday_byte.softseek.domain.model.AiChatRequest
import io.github.we007oneday_byte.softseek.domain.model.AiModelInfo
import io.github.we007oneday_byte.softseek.domain.model.BalanceInfo
import io.github.we007oneday_byte.softseek.domain.model.ChatError
import io.github.we007oneday_byte.softseek.domain.model.ChatFinishReason
import io.github.we007oneday_byte.softseek.domain.model.ChatMessage
import io.github.we007oneday_byte.softseek.domain.model.ChatSendEvent
import io.github.we007oneday_byte.softseek.domain.model.ChatStreamEvent
import io.github.we007oneday_byte.softseek.domain.model.CleanupResult
import io.github.we007oneday_byte.softseek.domain.model.Conversation
import io.github.we007oneday_byte.softseek.domain.model.MemoryPoolConfig
import io.github.we007oneday_byte.softseek.domain.model.MessageAttachment
import io.github.we007oneday_byte.softseek.domain.model.MessageStatus
import io.github.we007oneday_byte.softseek.domain.model.ProviderCapabilities
import io.github.we007oneday_byte.softseek.domain.model.ReasoningEffort
import io.github.we007oneday_byte.softseek.domain.model.TokenUsage
import io.github.we007oneday_byte.softseek.domain.provider.AiProvider
import io.github.we007oneday_byte.softseek.domain.repository.ConversationRepository
import io.github.we007oneday_byte.softseek.domain.repository.MemoryPoolRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class DefaultChatRepositoryTest {
    @Test
    fun sendMessageStreamsAndPersistsCompletedAssistant() = runTest {
        val conversationRepo = FakeConversationRepository()
        val conversationId = conversationRepo.createConversation("测试").id
        val provider = FakeProvider(
            events = listOf(
                ChatStreamEvent.ReasoningDelta("想"),
                ChatStreamEvent.ContentDelta("你"),
                ChatStreamEvent.ContentDelta("好"),
                ChatStreamEvent.UsageReceived(TokenUsage(10, 2, 12, 1)),
                ChatStreamEvent.Finished(ChatFinishReason.STOP),
                ChatStreamEvent.Completed,
            ),
        )
        val repository = buildRepository(conversationRepo, provider, apiKey = "sk-test")

        val events = repository.sendMessage(conversationId, "你好").toList()

        val started = events.filterIsInstance<ChatSendEvent.Started>().single()
        assertEquals("你好", started.userMessage?.content)
        assertEquals(MessageStatus.PARTIAL, started.assistantMessage.status)
        val finished = events.filterIsInstance<ChatSendEvent.Finished>().single().assistantMessage
        assertEquals("你好", finished.content)
        assertEquals("想", finished.reasoningContent)
        assertEquals(MessageStatus.COMPLETE, finished.status)
        assertEquals("stop", finished.finishReason)
        assertEquals(12, finished.totalTokens)
        assertEquals(1, finished.reasoningTokens)

        val stored = conversationRepo.getMessages(conversationId)
        assertEquals(2, stored.size)
        assertEquals("你好", stored.last().content)
        assertEquals(MessageStatus.COMPLETE, stored.last().status)
    }

    @Test
    fun missingApiKeyDoesNotCreateMessages() = runTest {
        val conversationRepo = FakeConversationRepository()
        val conversationId = conversationRepo.createConversation("测试").id
        val repository = buildRepository(conversationRepo, FakeProvider(), apiKey = null)

        val events = repository.sendMessage(conversationId, "你好").toList()

        assertEquals(1, events.size)
        assertEquals(ChatError.NoApiKey, (events.single() as ChatSendEvent.Failed).error)
        assertTrue(conversationRepo.getMessages(conversationId).isEmpty())
    }

    @Test
    fun regenerateSoftDeletesPreviousAssistantAndCreatesNewOne() = runTest {
        val conversationRepo = FakeConversationRepository()
        val conversationId = conversationRepo.createConversation("测试").id
        val provider = FakeProvider(
            events = listOf(
                ChatStreamEvent.ContentDelta("旧回复"),
                ChatStreamEvent.Finished(ChatFinishReason.STOP),
                ChatStreamEvent.Completed,
            ),
        )
        val repository = buildRepository(conversationRepo, provider, apiKey = "sk-test")
        repository.sendMessage(conversationId, "问题").toList()

        provider.events = listOf(
            ChatStreamEvent.ContentDelta("新回复"),
            ChatStreamEvent.Finished(ChatFinishReason.STOP),
            ChatStreamEvent.Completed,
        )
        val events = repository.regenerate(conversationId).toList()

        val started = events.filterIsInstance<ChatSendEvent.Started>().single()
        assertNull(started.userMessage)
        val finished = events.filterIsInstance<ChatSendEvent.Finished>().single().assistantMessage
        assertEquals("新回复", finished.content)
        assertEquals(2, conversationRepo.getMessages(conversationId).size)
        assertEquals("新回复", conversationRepo.getMessages(conversationId).last().content)
    }

    private fun buildRepository(
        conversationRepository: ConversationRepository,
        provider: AiProvider,
        apiKey: String?,
    ): DefaultChatRepository = DefaultChatRepository(
        provider = provider,
        conversationRepository = conversationRepository,
        memoryPoolRepository = FakeMemoryPoolRepository(),
        contextAssembler = ContextAssembler(TokenEstimator()),
        apiKeyStore = FakeApiKeyStore(apiKey),
    )
}

private class FakeApiKeyStore(initial: String?) : ApiKeyStore {
    private val state = MutableStateFlow(initial)
    private val summaries = MutableStateFlow<List<ApiKeySummary>>(emptyList())
    override val apiKey: StateFlow<String?> = state.asStateFlow()
    override val keySummaries: StateFlow<List<ApiKeySummary>> = summaries.asStateFlow()

    override fun setApiKey(value: String?) {
        state.value = value
    }

    override fun addKey(label: String, key: String, note: String?): String {
        state.value = key
        return "fake"
    }

    override fun activateKey(id: String) = Unit

    override fun updateKey(id: String, label: String, key: String?, note: String?) {
        if (!key.isNullOrBlank()) state.value = key
    }

    override fun deleteKey(id: String) {
        state.value = null
    }
}

private class FakeProvider(
    var events: List<ChatStreamEvent> = emptyList(),
) : AiProvider {
    override val id: String = "fake"
    override val displayName: String = "Fake"
    override val capabilities: ProviderCapabilities = ProviderCapabilities()

    override fun streamChat(apiKey: String, request: AiChatRequest): Flow<ChatStreamEvent> =
        flowOf(*events.toTypedArray())

    override suspend fun listModels(apiKey: String): List<AiModelInfo> = emptyList()

    override suspend fun getBalance(apiKey: String): BalanceInfo? = null
}

private class FakeMemoryPoolRepository : MemoryPoolRepository {
    private val configs = mutableMapOf<String, MemoryPoolConfig>()

    override fun observeConfig(poolId: String): Flow<MemoryPoolConfig?> = flowOf(configs[poolId])

    override suspend fun getConfig(poolId: String): MemoryPoolConfig =
        configs.getOrPut(poolId) { MemoryPoolConfig.defaults(poolId, 1L) }

    override suspend fun upsertConfig(config: MemoryPoolConfig) {
        configs[config.poolId] = config
    }

    override suspend fun cleanupNow(poolId: String, preview: Boolean, now: Long): CleanupResult =
        CleanupResult(poolId = poolId, preview = preview, candidates = emptyList(), deletedCount = 0)

    override suspend fun autoCleanup(poolId: String, now: Long): CleanupResult =
        CleanupResult(poolId = poolId, preview = false, candidates = emptyList(), deletedCount = 0)

    override suspend fun purgeExpired(now: Long): Int = 0
}

private class FakeConversationRepository : ConversationRepository {
    private val conversationState = MutableStateFlow<List<Conversation>>(emptyList())
    private val messageState = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())

    override fun observeConversations(includeDeleted: Boolean): Flow<List<Conversation>> =
        conversationState.map { list -> if (includeDeleted) list else list.filterNot { it.isDeleted } }

    override suspend fun getConversation(poolId: String): Conversation? =
        conversationState.value.firstOrNull { it.id == poolId }

    override suspend fun createConversation(
        title: String,
        modelName: String,
        reasoningEffort: ReasoningEffort,
        maxOutputTokens: Int,
        systemPrompt: String?,
        now: Long,
    ): Conversation {
        val conversation = Conversation(
            id = UUID.randomUUID().toString(),
            title = title,
            systemPrompt = systemPrompt,
            modelName = modelName,
            reasoningEffort = reasoningEffort,
            maxOutputTokens = maxOutputTokens,
            tags = emptyList(),
            isPinned = false,
            isDeleted = false,
            createdAt = now,
            updatedAt = now,
            lastMessageAt = null,
            lastMessagePreview = null,
            deletedAt = null,
        )
        conversationState.update { it + conversation }
        return conversation
    }

    override suspend fun renameConversation(poolId: String, title: String, now: Long) {
        conversationState.update { list ->
            list.map { if (it.id == poolId) it.copy(title = title, updatedAt = now) else it }
        }
    }

    override suspend fun setPinned(poolId: String, pinned: Boolean, now: Long) {
        conversationState.update { list ->
            list.map { if (it.id == poolId) it.copy(isPinned = pinned, updatedAt = now) else it }
        }
    }

    override suspend fun updateConfig(
        poolId: String,
        modelName: String,
        reasoningEffort: ReasoningEffort,
        maxOutputTokens: Int,
        now: Long,
    ) {
        conversationState.update { list ->
            list.map {
                if (it.id == poolId) {
                    it.copy(
                        modelName = modelName,
                        reasoningEffort = reasoningEffort,
                        maxOutputTokens = maxOutputTokens,
                        updatedAt = now,
                    )
                } else {
                    it
                }
            }
        }
    }

    override suspend fun softDeleteConversation(poolId: String, now: Long) {
        conversationState.update { list ->
            list.map { if (it.id == poolId) it.copy(isDeleted = true, deletedAt = now) else it }
        }
    }

    override suspend fun restoreConversation(poolId: String, now: Long) {
        conversationState.update { list ->
            list.map { if (it.id == poolId) it.copy(isDeleted = false, deletedAt = null) else it }
        }
    }

    override fun observeMessages(poolId: String): Flow<List<ChatMessage>> =
        messageState.map { it[poolId].orEmpty().filterNot { message -> message.isDeleted } }

    override suspend fun getMessages(poolId: String): List<ChatMessage> =
        messageState.value[poolId].orEmpty().filterNot { it.isDeleted }.sortedBy { it.timestamp }

    override suspend fun addMessage(message: ChatMessage) {
        messageState.update { current ->
            current + (message.conversationId to (current[message.conversationId].orEmpty() + message))
        }
    }

    override suspend fun updateMessage(message: ChatMessage) {
        messageState.update { current ->
            val list = current[message.conversationId].orEmpty()
            current + (message.conversationId to list.map { if (it.id == message.id) message else it })
        }
    }

    override suspend fun softDeleteMessage(nodeId: String, now: Long) {
        messageState.update { current ->
            current.mapValues { (_, list) ->
                list.map { if (it.id == nodeId) it.copy(isDeleted = true, deletedAt = now) else it }
            }
        }
    }

    override suspend fun addAttachment(attachment: MessageAttachment) = Unit

    override suspend fun getAttachments(nodeId: String): List<MessageAttachment> = emptyList()
}
