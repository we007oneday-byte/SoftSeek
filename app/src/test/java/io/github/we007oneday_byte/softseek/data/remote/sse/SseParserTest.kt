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

class SseParserTest {
    private val parser = SseParser(
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        },
    )

    @Test
    fun parsesReasoningContentFinishUsageAndDone() = runTest {
        val payload = """
            data: {"choices":[{"index":0,"delta":{"role":"assistant","reasoning_content":"先想"},"finish_reason":null}]}

            data: {"choices":[{"index":0,"delta":{"content":"你好"},"finish_reason":null}]}

            data: {"choices":[{"index":0,"delta":{},"finish_reason":"stop"}],"usage":{"prompt_tokens":10,"completion_tokens":2,"total_tokens":12,"completion_tokens_details":{"reasoning_tokens":1}}}

            data: [DONE]

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
            ChatFinishReason.STOP,
            events.filterIsInstance<ChatStreamEvent.Finished>().single().reason,
        )
        assertEquals(
            TokenUsage(
                promptTokens = 10,
                completionTokens = 2,
                totalTokens = 12,
                reasoningTokens = 1,
                cacheHitTokens = 0,
                cacheMissTokens = 0,
            ),
            events.filterIsInstance<ChatStreamEvent.UsageReceived>().single().usage,
        )
        assertTrue(events.last() is ChatStreamEvent.Completed)
    }

    @Test
    fun emitsProtocolErrorForMalformedChunk() = runTest {
        val events = parser.parse(Buffer().writeUtf8("data: {\"choices\":[\n\n")).toList()

        val failed = events.filterIsInstance<ChatStreamEvent.Failed>().single()
        assertTrue(failed.error is ChatError.Protocol)
    }

    @Test
    fun parsesApiErrorPayloadAsBadRequest() = runTest {
        val events = parser.parse(
            Buffer().writeUtf8(
                "data: {\"error\":{\"message\":\"invalid api key\",\"type\":\"authentication_error\"}}\n\n",
            ),
        ).toList()

        val failed = events.filterIsInstance<ChatStreamEvent.Failed>().single()
        assertEquals(ChatError.BadRequest("invalid api key"), failed.error)
    }

    @Test
    fun mapsFinishReasons() = runTest {
        val reasons = listOf(
            "length" to ChatFinishReason.LENGTH,
            "content_filter" to ChatFinishReason.CONTENT_FILTER,
            "insufficient_system_resource" to ChatFinishReason.INSUFFICIENT_SYSTEM_RESOURCE,
            "aborted" to ChatFinishReason.ABORTED,
            "something_else" to ChatFinishReason.UNKNOWN,
        )
        for ((raw, expected) in reasons) {
            val event = parser.parse(
                Buffer().writeUtf8(
                    "data: {\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"$raw\"}]}\n\n",
                ),
            ).toList().filterIsInstance<ChatStreamEvent.Finished>().single()
            assertEquals(expected, event.reason)
        }
    }

    @Test
    fun doneWithoutPayloadEmitsCompleted() = runTest {
        val events = parser.parse(Buffer().writeUtf8("data: [DONE]\n\n")).toList()
        assertEquals(listOf(ChatStreamEvent.Completed), events)
    }
}
