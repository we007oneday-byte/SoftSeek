package io.github.we007oneday_byte.softseek.data.local

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.we007oneday_byte.softseek.data.local.dao.MemoryHeapNodeDao
import io.github.we007oneday_byte.softseek.data.local.dao.MemoryPoolBlockDao
import io.github.we007oneday_byte.softseek.data.local.dao.MemoryPoolConfigDao
import io.github.we007oneday_byte.softseek.data.local.dao.MessageAttachmentDao
import io.github.we007oneday_byte.softseek.data.local.entity.MemoryHeapNodeEntity
import io.github.we007oneday_byte.softseek.data.local.entity.MemoryPoolBlockEntity
import io.github.we007oneday_byte.softseek.data.local.entity.MemoryPoolConfigEntity
import io.github.we007oneday_byte.softseek.data.local.entity.MessageAttachmentEntity
import io.github.we007oneday_byte.softseek.domain.model.AttachmentType
import io.github.we007oneday_byte.softseek.domain.model.MessageRole
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
class DatabaseDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var blockDao: MemoryPoolBlockDao
    private lateinit var nodeDao: MemoryHeapNodeDao
    private lateinit var attachmentDao: MessageAttachmentDao
    private lateinit var configDao: MemoryPoolConfigDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        blockDao = database.memoryPoolBlockDao()
        nodeDao = database.memoryHeapNodeDao()
        attachmentDao = database.messageAttachmentDao()
        configDao = database.memoryPoolConfigDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun blockLifecycleAndQueries() = runTest {
        blockDao.upsert(block("pool-1", now = 100L))
        blockDao.upsert(block("pool-2", now = 200L, pinned = true))

        assertEquals(2, blockDao.observeActive().first().size)
        assertEquals("pool-2", blockDao.observeActive().first().first().poolId)
        assertNotNull(blockDao.getById("pool-1"))

        blockDao.rename("pool-1", "改名", 300L)
        assertEquals("改名", blockDao.getById("pool-1")?.title)

        blockDao.setPinned("pool-1", true, 301L)
        assertTrue(blockDao.getById("pool-1")?.isPinned == true)

        blockDao.softDelete("pool-1", deletedAt = 400L, updatedAt = 400L)
        assertEquals(1, blockDao.observeDeleted().first().size)
        blockDao.restore("pool-1", 500L)
        assertFalse(blockDao.getById("pool-1")?.isDeleted == true)

        blockDao.updateLastMessage("pool-1", 501L, "最后一条", 501L)
        assertEquals("最后一条", blockDao.getById("pool-1")?.lastMessagePreview)
    }

    @Test
    fun nodeAndAttachmentRelation() = runTest {
        blockDao.upsert(block("pool-1", now = 100L))
        nodeDao.upsert(node("node-1", "pool-1", timestamp = 10L))
        nodeDao.upsert(node("node-2", "pool-1", timestamp = 20L))
        attachmentDao.upsert(attachment("att-1", "node-1"))

        val messages = nodeDao.observeWithAttachments("pool-1").first()
        assertEquals(2, messages.size)
        assertEquals(1, messages.first().attachments.size)
        assertEquals("att-1", messages.first().attachments.first().attachmentId)
        assertEquals(1, attachmentDao.getByNode("node-1").size)

        val candidates = nodeDao.getCleanupCandidates(
            poolId = "pool-1",
            keepCount = 1,
            protectionCutoff = 100L,
        )
        assertEquals(1, candidates.size)
        assertEquals("node-1", candidates.first().nodeId)

        nodeDao.softDeleteByIds(listOf("node-1"), 30L)
        assertEquals(1, nodeDao.countActive("pool-1"))
        assertEquals(2, nodeDao.getAllByPool("pool-1").size)
    }

    @Test
    fun configRoundTripAndCascadeDelete() = runTest {
        blockDao.upsert(block("pool-1", now = 100L))
        configDao.upsert(
            MemoryPoolConfigEntity(
                poolId = "pool-1",
                updatedAt = 100L,
                enableAutoCleanup = true,
                cleanupThresholdCount = 3,
                tokenEstimateRatio = 1.8,
            ),
        )
        nodeDao.upsert(node("node-1", "pool-1", timestamp = 10L))
        attachmentDao.upsert(attachment("att-1", "node-1"))

        val config = configDao.get("pool-1")
        assertNotNull(config)
        assertTrue(config?.enableAutoCleanup == true)
        assertEquals(3, config?.cleanupThresholdCount)
        assertEquals(1.8, config?.tokenEstimateRatio ?: 0.0, 0.001)

        blockDao.deleteById("pool-1")
        assertEquals(0, nodeDao.getAllByPool("pool-1").size)
        assertEquals(0, attachmentDao.getByNode("node-1").size)
        assertEquals(null, configDao.get("pool-1"))
    }

    private fun block(id: String, now: Long, pinned: Boolean = false): MemoryPoolBlockEntity =
        MemoryPoolBlockEntity(
            poolId = id,
            title = "对话$id",
            createdAt = now,
            updatedAt = now,
            isPinned = pinned,
            tags = listOf("测试", "room"),
        )

    private fun node(id: String, poolId: String, timestamp: Long): MemoryHeapNodeEntity =
        MemoryHeapNodeEntity(
            nodeId = id,
            poolId = poolId,
            role = MessageRole.USER,
            content = "内容$id",
            timestamp = timestamp,
        )

    private fun attachment(id: String, nodeId: String): MessageAttachmentEntity =
        MessageAttachmentEntity(
            attachmentId = id,
            nodeId = nodeId,
            type = AttachmentType.IMAGE,
            localPath = "/tmp/$id.png",
            mime = "image/png",
            width = 100,
            height = 100,
            sizeBytes = 1024,
        )
}
