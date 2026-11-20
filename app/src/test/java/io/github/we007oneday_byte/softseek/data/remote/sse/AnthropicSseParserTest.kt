package io.github.we007oneday_byte.softseek.data.remote.sse

import io.github.we007oneday_byte.softseek.domain.model.ChatError
import io.github.we007oneday_byte.softseek.domain.model.ChatFinishReason
import io.github.we007oneday_byte.softseek.domain.model.ChatStreamEvent
import io.github.we007oneday_byte.softseek.domain.model.TokenUsage
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnthropicSseParserTest {
    private val parser = AnthropicSseParser(
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        },
    )

    @Test
    fun parsesTextThinkingUsageFinishAndStop() = runTest {
        val payload = """
            event: message_start
            data: {"type":"message_start","message":{"id":"msg_1","model":"claude","usage":{"input_tokens":10,"cache_read_input_tokens":2}}}

            event: content_block_delta
            data: {"type":"content_block_delta","index":0,"delta":{"type":"thinking_delta","thinking":"先想"}}

            event: content_block_delta
            data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"你好"}}

            event: message_delta
            data: {"type":"message_delta","delta":{"stop_reason":"end_turn"},"usage":{"output_tokens":3}}

            event: message_stop
            data: {"type":"message_stop"}

        """.trimIndent()

        val events = parser.parse(Buffer().writeUtf8(payload)).toList()

        assertEquals(
            listOf("先想"),
            events.filterIsInstance<ChatStreamEvent.ReasoningDelta>().map { it.text },
        )
        assertEquals(
            listOf("你好"),
            events.filterIsInstance<ChatStreamEvent.ContentDelta>().map { it.text },
        )
        assertEquals(
            TokenUsage(
                promptTokens = 10,
                completionTokens = 3,
                totalTokens = 13,
                cacheHitTokens = 2,
            ),
            events.filterIsInstance<ChatStreamEvent.UsageReceived>().single().usage,
        )
        assertEquals(
            ChatFinishReason.STOP,
            events.filterIsInstance<ChatStreamEvent.Finished>().single().reason,
        )
        assertTrue(events.last() is ChatStreamEvent.Completed)
    }

    @Test
    fun parsesErrorEventAsBadRequest() = runTest {
        val events = parser.parse(
            Buffer().writeUtf8(
                "event: error\n" +
                    "data: {\"type\":\"error\",\"error\":{\"type\":\"authentication_error\"," +
                    "\"message\":\"invalid api key\"}}\n\n",
            ),
        ).toList()

        val failed = events.filterIsInstance<ChatStreamEvent.Failed>().single()
        assertEquals(ChatError.BadRequest("invalid api key"), failed.error)
    }

    @Test
    fun mapsMaxTokensFinishReason() = runTest {
        val events = parser.parse(
            Buffer().writeUtf8(
                "data: {\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"max_tokens\"}," +
                    "\"usage\":{\"output_tokens\":1}}\n\n",
            ),
        ).toList()

        assertEquals(
            ChatFinishReason.LENGTH,
            events.filterIsInstance<ChatStreamEvent.Finished>().single().reason,
        )
    }
}
