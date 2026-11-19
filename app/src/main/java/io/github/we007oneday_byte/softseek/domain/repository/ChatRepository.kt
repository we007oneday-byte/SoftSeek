package io.github.we007oneday_byte.softseek.domain.repository

import io.github.we007oneday_byte.softseek.domain.model.ChatSendEvent
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun sendMessage(conversationId: String, text: String): Flow<ChatSendEvent>

    fun regenerate(conversationId: String): Flow<ChatSendEvent>
}
