package io.github.we007oneday_byte.softseek.domain.model

sealed interface ChatStreamEvent {
    data class ContentDelta(val text: String) : ChatStreamEvent

    data class ReasoningDelta(val text: String) : ChatStreamEvent

    data class UsageReceived(val usage: TokenUsage) : ChatStreamEvent

    data class Finished(val reason: ChatFinishReason) : ChatStreamEvent

    data class Failed(val error: ChatError) : ChatStreamEvent

    data object Completed : ChatStreamEvent
}
