package io.github.we007oneday_byte.softseek.data.repository

import io.github.we007oneday_byte.softseek.data.local.dao.MemoryHeapNodeDao
import io.github.we007oneday_byte.softseek.data.local.dao.MemoryPoolBlockDao
import io.github.we007oneday_byte.softseek.data.local.dao.MemoryPoolConfigDao
import io.github.we007oneday_byte.softseek.data.local.dao.MessageAttachmentDao
import io.github.we007oneday_byte.softseek.data.local.entity.MemoryPoolBlockEntity
import io.github.we007oneday_byte.softseek.domain.model.ChatMessage
import io.github.we007oneday_byte.softseek.domain.model.Conversation
import io.github.we007oneday_byte.softseek.domain.model.MemoryPoolConfig
import io.github.we007oneday_byte.softseek.domain.model.MessageAttachment
import io.github.we007oneday_byte.softseek.domain.model.MessageRole
import io.github.we007oneday_byte.softseek.domain.model.ReasoningEffort
import io.github.we007oneday_byte.softseek.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomConversationRepository
    @Inject
    constructor(
        private val blockDao: MemoryPoolBlockDao,
        private val nodeDao: MemoryHeapNodeDao,
        private val attachmentDao: MessageAttachmentDao,
        private val configDao: MemoryPoolConfigDao,
    ) : ConversationRepository {
        override fun observeConversations(includeDeleted: Boolean): Flow<List<Conversation>> {
            val source = if (includeDeleted) blockDao.observeAll() else blockDao.observeActive()
            return source.map { blocks -> blocks.map { it.toDomain() } }
        }

        override suspend fun getConversation(poolId: String): Conversation? =
            blockDao.getById(poolId)?.toDomain()

        override suspend fun createConversation(
            title: String,
            modelName: String,
            reasoningEffort: ReasoningEffort,
            maxOutputTokens: Int,
            systemPrompt: String?,
            now: Long,
        ): Conversation {
            val poolId = UUID.randomUUID().toString()
            val block = MemoryPoolBlockEntity(
                poolId = poolId,
                title = title,
                createdAt = now,
                updatedAt = now,
                systemPrompt = systemPrompt,
                modelName = modelName,
                reasoningEffort = reasoningEffort,
                maxOutputTokens = maxOutputTokens,
            )
            blockDao.upsert(block)
            configDao.upsert(MemoryPoolConfig.defaults(poolId, now).toEntity())
            return block.toDomain()
        }

        override suspend fun renameConversation(poolId: String, title: String, now: Long) {
            blockDao.rename(poolId, title, now)
        }

        override suspend fun setPinned(poolId: String, pinned: Boolean, now: Long) {
            blockDao.setPinned(poolId, pinned, now)
        }

        override suspend fun softDeleteConversation(poolId: String, now: Long) {
            blockDao.softDelete(poolId, now, now)
        }

        override suspend fun restoreConversation(poolId: String, now: Long) {
            blockDao.restore(poolId, now)
        }

        override fun observeMessages(poolId: String): Flow<List<ChatMessage>> =
            nodeDao.observeWithAttachments(poolId).map { nodes -> nodes.map { it.toDomain() } }

        override suspend fun getMessages(poolId: String): List<ChatMessage> =
            nodeDao.getWithAttachments(poolId).map { it.toDomain() }

        override suspend fun addMessage(message: ChatMessage) {
            nodeDao.upsert(message.toEntity())
            val attachments = message.attachments.map { it.copy(nodeId = message.id) }
            if (attachments.isNotEmpty()) {
                attachmentDao.upsertAll(attachments.map { it.toEntity() })
            }
            if (message.role != MessageRole.SYSTEM && !message.isDeleted) {
                val preview = message.content.take(PREVIEW_LENGTH)
                if (preview.isNotBlank() || message.attachments.isNotEmpty()) {
                    blockDao.updateLastMessage(
                        poolId = message.conversationId,
                        lastMessageAt = message.timestamp,
                        preview = preview.ifBlank { ATTACHMENT_PREVIEW },
                        updatedAt = message.timestamp,
                    )
                }
            }
        }

        override suspend fun updateMessage(message: ChatMessage) {
            nodeDao.upsert(message.toEntity())
        }

        override suspend fun softDeleteMessage(nodeId: String, now: Long) {
            nodeDao.softDeleteByIds(listOf(nodeId), now)
        }

        override suspend fun addAttachment(attachment: MessageAttachment) {
            attachmentDao.upsert(attachment.toEntity())
        }

        override suspend fun getAttachments(nodeId: String): List<MessageAttachment> =
            attachmentDao.getByNode(nodeId).map { it.toDomain() }

        private companion object {
            const val PREVIEW_LENGTH = 80
            const val ATTACHMENT_PREVIEW = "[附件]"
        }
    }
