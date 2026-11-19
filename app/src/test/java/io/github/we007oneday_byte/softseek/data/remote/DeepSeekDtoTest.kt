package io.github.we007oneday_byte.softseek.data.remote

import io.github.we007oneday_byte.softseek.data.remote.dto.BalanceResponseDto
import io.github.we007oneday_byte.softseek.data.remote.dto.ChatCompletionChunkDto
import io.github.we007oneday_byte.softseek.data.remote.dto.ChatCompletionRequestDto
import io.github.we007oneday_byte.softseek.data.remote.dto.ChatMessageDto
import io.github.we007oneday_byte.softseek.data.remote.dto.ModelsResponseDto
import io.github.we007oneday_byte.softseek.data.remote.dto.StreamOptionsDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepSeekDtoTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    @Test
    fun parsesModelCatalogWithEffortLevels() {
        val response = json.decodeFromString<ModelsResponseDto>(
            """
            {"data":[{"id":"deepseek-flash","name":"DeepSeek-V4.1-Flash","context_window":1048576,
            "max_output_tokens":393216,"input_modalities":["text","image"],
            "reasoning_effort":{"supported_levels":["low","high","max"],"default":"high"}}]}
            """.trimIndent(),
        )

        val model = response.data.single()
        assertEquals("deepseek-flash", model.id)
        assertEquals(1048576, model.contextWindow)
        assertEquals(393216, model.maxOutputTokens)
        assertEquals(listOf("text", "image"), model.inputModalities)
        assertEquals(listOf("low", "high", "max"), model.reasoningEffort?.supportedLevels)
    }

    @Test
    fun serializesReasoningAndStreamUsageOptions() {
        val request = ChatCompletionRequestDto(
            model = "deepseek-flash",
            messages = listOf(ChatMessageDto("user", "你好")),
            reasoningEffort = "high",
            maxTokens = 8192,
            stream = true,
            streamOptions = StreamOptionsDto(includeUsage = true),
        )
        val encoded = json.encodeToString(request)

        assertTrue(encoded.contains("\"reasoning_effort\":\"high\""))
        assertTrue(encoded.contains("\"max_tokens\":8192"))
        assertTrue(encoded.contains("\"include_usage\":true"))
    }

    @Test
    fun parsesUsageWithReasoningAndCacheTokens() {
        val chunk = json.decodeFromString<ChatCompletionChunkDto>(
            """
            {"choices":[],"usage":{"prompt_tokens":100,"completion_tokens":20,"total_tokens":120,
            "prompt_cache_hit_tokens":80,"prompt_cache_miss_tokens":20,
            "completion_tokens_details":{"reasoning_tokens":5}}}
            """.trimIndent(),
        )

        assertEquals(100, chunk.usage?.promptTokens)
        assertEquals(5, chunk.usage?.completionTokensDetails?.reasoningTokens)
        assertEquals(80, chunk.usage?.promptCacheHitTokens)
    }

    @Test
    fun parsesBalanceInfo() {
        val response = json.decodeFromString<BalanceResponseDto>(
            """
            {"is_available":true,"balance_infos":[{"currency":"CNY","total_balance":"10.00",
            "granted_balance":"2.00","topped_up_balance":"8.00"}]}
            """.trimIndent(),
        )

        assertTrue(response.isAvailable)
        assertEquals("CNY", response.balanceInfos.single().currency)
        assertEquals("10.00", response.balanceInfos.single().totalBalance)
    }
}
