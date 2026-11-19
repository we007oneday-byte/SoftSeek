package io.github.we007oneday_byte.softseek.data.repository

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.we007oneday_byte.softseek.data.local.AppDatabase
import io.github.we007oneday_byte.softseek.domain.model.AttachmentType
import io.github.we007oneday_byte.softseek.domain.model.ChatMessage
import io.github.we007oneday_byte.softseek.domain.model.MessageAttachment
import io.github.we007oneday_byte.softseek.domain.model.MessageRole
import io.github.we007oneday_byte.softseek.domain.model.MessageStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class ConversationRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: RoomConversationRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomConversationRepository(
            blockDao = database.memoryPoolBlockDao(),
            nodeDao = database.memoryHeapNodeDao(),
            attachmentDao = database.messageAttachmentDao(),
            configDao = database.memoryPoolConfigDao(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun conversationLifecycleAndMessages() = runTest {
        val conversation = repository.createConversation("新对话", now = 1000L)
        assertEquals("新对话", conversation.title)
        assertEquals(2, database.memoryPoolConfigDao().get(conversation.id)?.cleanupThresholdCount)
        assertEquals(false, database.memoryPoolConfigDao().get(conversation.id)?.enableAutoCleanup)

        repository.addMessage(
            message(
                id = "m1",
                poolId = conversation.id,
                role = MessageRole.USER,
                content = "你好",
                timestamp = 1001L,
                attachments = listOf(attachment("a1", "m1")),
            ),
        )
        assertEquals("你好", repository.getConversation(conversation.id)?.lastMessagePreview)
        val firstMessages = repository.observeMessages(conversation.id).first()
        assertEquals(1, firstMessages.size)
        assertEquals(1, firstMessages.first().attachments.size)

        repository.addMessage(
            message(
                id = "m2",
                poolId = conversation.id,
                role = MessageRole.ASSISTANT,
                content = "你好，我是 SOFTSEEK",
                timestamp = 1002L,
                reasoningContent = "内部思考",
            ),
        )
        val messages = repository.getMessages(conversation.id)
        assertEquals(2, messages.size)
        assertEquals("内部思考", messages.last().reasoningContent)
        assertEquals("你好，我是 SOFTSEEK", repository.getConversation(conversation.id)?.lastMessagePreview)

        repository.renameConversation(conversation.id, "重命名", now = 1003L)
        assertEquals("重命名", repository.getConversation(conversation.id)?.title)

        repository.setPinned(conversation.id, true, now = 1004L)
        assertTrue(repository.getConversation(conversation.id)?.isPinned == true)

        repository.softDeleteConversation(conversation.id, now = 1005L)
        assertTrue(repository.getConversation(conversation.id)?.isDeleted == true)
        assertEquals(0, repository.observeConversations().first().size)
        assertEquals(1, repository.observeConversations(includeDeleted = true).first().size)

        repository.restoreConversation(conversation.id, now = 1006L)
        assertFalse(repository.getConversation(conversation.id)?.isDeleted == true)
        assertEquals(1, repository.observeConversations().first().size)
    }

    @Test
    fun messageUpdateAndSoftDelete() = runTest {
        val conversation = repository.createConversation("消息测试", now = 2000L)
        repository.addMessage(
            message("m1", conversation.id, MessageRole.USER, "旧内容", 2001L),
        )
        repository.updateMessage(
            message("m1", conversation.id, MessageRole.USER, "新内容", 2001L).copy(totalTokens = 10),
        )
        assertEquals("新内容", repository.getMessages(conversation.id).single().content)
        assertEquals(10, repository.getMessages(conversation.id).single().totalTokens)

        repository.softDeleteMessage("m1", now = 2002L)
        assertEquals(0, repository.getMessages(conversation.id).size)
    }

    @Test
    fun attachmentIsLoadedWithNode() = runTest {
        val conversation = repository.createConversation("附件测试", now = 3000L)
        repository.addMessage(
            message(
                id = "m1",
                poolId = conversation.id,
                role = MessageRole.USER,
                content = "看图",
                timestamp = 3001L,
                attachments = listOf(attachment("a1", "m1")),
            ),
        )
        val attachments = repository.getAttachments("m1")
        assertEquals(1, attachments.size)
        assertEquals(AttachmentType.IMAGE, attachments.single().type)
        assertNotNull(repository.getConversation(conversation.id))
    }

    private fun message(
        id: String,
        poolId: String,
        role: MessageRole,
        content: String,
        timestamp: Long,
        attachments: List<MessageAttachment> = emptyList(),
        reasoningContent: String? = null,
    ): ChatMessage = ChatMessage(
        id = id,
        conversationId = poolId,
        parentId = null,
        role = role,
        content = content,
        reasoningContent = reasoningContent,
        reasoningTokens = 0,
        modelName = "deepseek-flash",
        status = MessageStatus.COMPLETE,
        finishReason = "stop",
        errorMessage = null,
        promptTokens = 0,
        completionTokens = 0,
        totalTokens = 0,
        timestamp = timestamp,
        isDeleted = false,
        deletedAt = null,
        attachments = attachments,
    )

    private fun attachment(id: String, nodeId: String): MessageAttachment = MessageAttachment(
        attachmentId = id,
        nodeId = nodeId,
        type = AttachmentType.IMAGE,
        localPath = "/tmp/$id.png",
        mime = "image/png",
        width = 100,
        height = 100,
        sizeBytes = 1024,
        sortOrder = 0,
    )
}
