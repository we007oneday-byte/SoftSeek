package io.github.we007oneday_byte.softseek.domain.model

sealed interface ChatSendEvent {
    data class Started(
        val userMessage: ChatMessage?,
        val assistantMessage: ChatMessage,
    ) : ChatSendEvent

    data class Delta(
        val assistantMessageId: String,
        val content: String,
        val reasoning: String,
    ) : ChatSendEvent

    data class Finished(val assistantMessage: ChatMessage) : ChatSendEvent

    data class Failed(
        val assistantMessage: ChatMessage?,
        val error: ChatError,
    ) : ChatSendEvent

    data class Stopped(val assistantMessage: ChatMessage?) : ChatSendEvent
}
