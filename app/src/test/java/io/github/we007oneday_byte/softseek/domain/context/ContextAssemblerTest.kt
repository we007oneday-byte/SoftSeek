package io.github.we007oneday_byte.softseek.domain.context

import io.github.we007oneday_byte.softseek.domain.model.AttachmentType
import io.github.we007oneday_byte.softseek.domain.model.ChatMessage
import io.github.we007oneday_byte.softseek.domain.model.MessageAttachment
import io.github.we007oneday_byte.softseek.domain.model.MessageRole
import io.github.we007oneday_byte.softseek.domain.model.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextAssemblerTest {
    private val assembler = ContextAssembler(TokenEstimator())

    @Test
    fun keepsChronologicalOrderAndStripsReasoning() {
        val history = listOf(
            message("u1", MessageRole.USER, "第一问", 1L),
            message("a1", MessageRole.ASSISTANT, "第一答", 2L, reasoning = "不能进入上下文的思考"),
            message("u2", MessageRole.USER, "第二问", 3L),
        )
        val result = assembler.assemble("系统提示", history, maxInputTokens = 10_000)

        assertEquals(4, result.messages.size)
        assertEquals(MessageRole.SYSTEM, result.messages.first().role)
        assertEquals(listOf("第一问", "第一答", "第二问"), result.messages.drop(1).map { it.content })
        assertFalse(result.messages.any { it.content.contains("不能进入上下文") })
        assertTrue(result.estimatedInputTokens <= 10_000)
    }

    @Test
    fun tightBudgetTruncatesOlderContentAndKeepsLatest() {
        val history = listOf(
            message("u1", MessageRole.USER, "旧".repeat(200), 1L),
            message("u2", MessageRole.USER, "最新消息", 2L),
        )
        val result = assembler.assemble(null, history, maxInputTokens = 20)

        assertEquals("最新消息", result.messages.last().content)
        assertTrue(result.truncated)
        assertTrue(result.estimatedInputTokens <= 20)
    }

    @Test
    fun imageTokensAreCountedAt384Each() {
        val history = listOf(
            message("u1", MessageRole.USER, "hi", 1L, imageCount = 1),
        )
        val result = assembler.assemble(null, history, maxInputTokens = 400)

        assertEquals(1, result.messages.single().imageCount)
        assertEquals(386, result.messages.single().tokenEstimate)
        assertEquals(386, result.estimatedInputTokens)
    }

    @Test
    fun systemPromptIsTruncatedToBudget() {
        val result = assembler.assemble("系".repeat(100), emptyList(), maxInputTokens = 10)

        assertEquals(1, result.messages.size)
        assertEquals(MessageRole.SYSTEM, result.messages.single().role)
        assertEquals(10, result.estimatedInputTokens)
        assertTrue(result.truncated)
    }

    @Test
    fun failedAndDeletedMessagesAreExcluded() {
        val history = listOf(
            message("u1", MessageRole.USER, "有效", 1L),
            message("a1", MessageRole.ASSISTANT, "失败", 2L, status = MessageStatus.FAILED),
            message("u2", MessageRole.USER, "已删除", 3L, deleted = true),
        )
        val result = assembler.assemble(null, history, maxInputTokens = 1000)

        assertEquals(listOf("有效"), result.messages.map { it.content })
    }

    @Test
    fun partialAssistantIsNotSentBackAsContext() {
        val history = listOf(
            message("u1", MessageRole.USER, "问题", 1L),
            message("a1", MessageRole.ASSISTANT, "被中断的一半", 2L, status = MessageStatus.PARTIAL),
            message("u2", MessageRole.USER, "继续", 3L),
        )
        val result = assembler.assemble(null, history, maxInputTokens = 1000)

        assertEquals(listOf("问题", "继续"), result.messages.map { it.content })
    }

    private fun message(
        id: String,
        role: MessageRole,
        content: String,
        timestamp: Long,
        reasoning: String? = null,
        imageCount: Int = 0,
        status: MessageStatus = MessageStatus.COMPLETE,
        deleted: Boolean = false,
    ): ChatMessage = ChatMessage(
        id = id,
        conversationId = "pool-1",
        parentId = null,
        role = role,
        content = content,
        reasoningContent = reasoning,
        reasoningTokens = 0,
        modelName = "deepseek-flash",
        status = status,
        finishReason = "stop",
        errorMessage = null,
        promptTokens = 0,
        completionTokens = 0,
        totalTokens = 0,
        timestamp = timestamp,
        isDeleted = deleted,
        deletedAt = if (deleted) timestamp else null,
        attachments = (1..imageCount).map { index ->
            MessageAttachment(
                attachmentId = "$id-$index",
                nodeId = id,
                type = AttachmentType.IMAGE,
                localPath = "/tmp/$id-$index.png",
                mime = "image/png",
                width = 100,
                height = 100,
                sizeBytes = 1024,
                sortOrder = index,
            )
        },
    )
}
