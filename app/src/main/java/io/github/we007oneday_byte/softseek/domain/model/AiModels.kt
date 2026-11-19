package io.github.we007oneday_byte.softseek.domain.model

data class AiMessage(
    val role: MessageRole,
    val content: String,
)

data class AiChatRequest(
    val model: String,
    val messages: List<AiMessage>,
    val reasoningEffort: ReasoningEffort,
    val maxOutputTokens: Int,
    val stream: Boolean = true,
)

data class AiModelInfo(
    val id: String,
    val displayName: String,
    val contextWindow: Int,
    val maxOutputTokens: Int,
    val inputModalities: List<String>,
    val reasoningLevels: List<ReasoningEffort>,
    val defaultReasoningEffort: ReasoningEffort?,
)

data class ProviderCapabilities(
    val supportsStreaming: Boolean = true,
    val supportsReasoning: Boolean = true,
    val supportsVision: Boolean = false,
    val supportsBalance: Boolean = true,
    val supportsModelList: Boolean = true,
)

data class TokenUsage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
    val reasoningTokens: Int = 0,
    val cacheHitTokens: Int = 0,
    val cacheMissTokens: Int = 0,
)

data class BalanceInfo(
    val isAvailable: Boolean,
    val balances: List<BalanceEntry>,
)

data class BalanceEntry(
    val currency: String,
    val totalBalance: String,
    val grantedBalance: String,
    val toppedUpBalance: String,
)

enum class ChatFinishReason {
    STOP,
    LENGTH,
    CONTENT_FILTER,
    TOOL_CALLS,
    INSUFFICIENT_SYSTEM_RESOURCE,
    ABORTED,
    UNKNOWN,
}
