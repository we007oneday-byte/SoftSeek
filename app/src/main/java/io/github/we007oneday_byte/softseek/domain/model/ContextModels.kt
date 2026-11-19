package io.github.we007oneday_byte.softseek.domain.model

data class ContextMessage(
    val role: MessageRole,
    val content: String,
    val imageCount: Int,
    val tokenEstimate: Int,
    val images: List<MessageAttachment> = emptyList(),
)

data class ContextBuildResult(
    val messages: List<ContextMessage>,
    val estimatedInputTokens: Int,
    val truncated: Boolean,
)
