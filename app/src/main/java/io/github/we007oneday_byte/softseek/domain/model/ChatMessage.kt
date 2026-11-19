package io.github.we007oneday_byte.softseek.domain.model

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val parentId: String?,
    val role: MessageRole,
    val content: String,
    val reasoningContent: String?,
    val reasoningTokens: Int,
    val modelName: String?,
    val status: MessageStatus,
    val finishReason: String?,
    val errorMessage: String?,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val timestamp: Long,
    val isDeleted: Boolean,
    val deletedAt: Long?,
    val attachments: List<MessageAttachment>,
)

data class MessageAttachment(
    val attachmentId: String,
    val nodeId: String,
    val type: AttachmentType,
    val localPath: String,
    val mime: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val sortOrder: Int,
)
