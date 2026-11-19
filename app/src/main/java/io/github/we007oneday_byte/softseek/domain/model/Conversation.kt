package io.github.we007oneday_byte.softseek.domain.model

data class Conversation(
    val id: String,
    val title: String,
    val systemPrompt: String?,
    val modelName: String,
    val reasoningEffort: ReasoningEffort,
    val maxOutputTokens: Int,
    val tags: List<String>,
    val isPinned: Boolean,
    val isDeleted: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val lastMessageAt: Long?,
    val lastMessagePreview: String?,
    val deletedAt: Long?,
)
