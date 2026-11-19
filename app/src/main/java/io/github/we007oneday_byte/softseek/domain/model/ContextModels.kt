package io.github.we007oneday_byte.softseek.domain.model

data class ContextMessage(
    val role: MessageRole,
    val content: String,
    val imageCount: Int,
    val tokenEstimate: Int,
)

data class ContextBuildResult(
    val messages: List<ContextMessage>,
    val estimatedInputTokens: Int,
    val truncated: Boolean,
)
