package io.github.we007oneday_byte.softseek.domain.repository

import io.github.we007oneday_byte.softseek.domain.model.ChatMessage
import io.github.we007oneday_byte.softseek.domain.model.Conversation
import io.github.we007oneday_byte.softseek.domain.model.MessageAttachment
import io.github.we007oneday_byte.softseek.domain.model.ReasoningEffort
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    fun observeConversations(includeDeleted: Boolean = false): Flow<List<Conversation>>

    suspend fun getConversation(poolId: String): Conversation?

    suspend fun createConversation(
        title: String,
        modelName: String = "deepseek-flash",
        reasoningEffort: ReasoningEffort = ReasoningEffort.HIGH,
        maxOutputTokens: Int = 8192,
        systemPrompt: String? = null,
        now: Long = System.currentTimeMillis(),
    ): Conversation

    suspend fun renameConversation(poolId: String, title: String, now: Long = System.currentTimeMillis())

    suspend fun setPinned(poolId: String, pinned: Boolean, now: Long = System.currentTimeMillis())

    suspend fun updateConfig(
        poolId: String,
        modelName: String,
        reasoningEffort: ReasoningEffort,
        maxOutputTokens: Int,
        now: Long = System.currentTimeMillis(),
    )

    suspend fun softDeleteConversation(poolId: String, now: Long = System.currentTimeMillis())

    suspend fun restoreConversation(poolId: String, now: Long = System.currentTimeMillis())

    fun observeMessages(poolId: String): Flow<List<ChatMessage>>

    suspend fun getMessages(poolId: String): List<ChatMessage>

    suspend fun addMessage(message: ChatMessage)

    suspend fun updateMessage(message: ChatMessage)

    suspend fun softDeleteMessage(nodeId: String, now: Long = System.currentTimeMillis())

    suspend fun addAttachment(attachment: MessageAttachment)

    suspend fun getAttachments(nodeId: String): List<MessageAttachment>
}
