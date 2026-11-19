package io.github.we007oneday_byte.softseek.data.repository

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.we007oneday_byte.softseek.data.cleanup.CleanupEngine
import io.github.we007oneday_byte.softseek.data.local.AppDatabase
import io.github.we007oneday_byte.softseek.data.local.dao.MemoryHeapNodeDao
import io.github.we007oneday_byte.softseek.data.local.dao.MemoryPoolBlockDao
import io.github.we007oneday_byte.softseek.data.local.dao.MemoryPoolConfigDao
import io.github.we007oneday_byte.softseek.data.local.entity.MemoryHeapNodeEntity
import io.github.we007oneday_byte.softseek.data.local.entity.MemoryPoolBlockEntity
import io.github.we007oneday_byte.softseek.data.local.entity.MemoryPoolConfigEntity
import io.github.we007oneday_byte.softseek.domain.model.CleanupSkipReason
import io.github.we007oneday_byte.softseek.domain.model.MessageRole
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class MemoryPoolRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var blockDao: MemoryPoolBlockDao
    private lateinit var nodeDao: MemoryHeapNodeDao
    private lateinit var configDao: MemoryPoolConfigDao
    private lateinit var repository: RoomMemoryPoolRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        blockDao = database.memoryPoolBlockDao()
        nodeDao = database.memoryHeapNodeDao()
        configDao = database.memoryPoolConfigDao()
        repository = RoomMemoryPoolRepository(
            blockDao = blockDao,
            configDao = configDao,
            cleanupEngine = CleanupEngine(blockDao, nodeDao, configDao),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun automaticCleanupIsDisabledByDefault() = runTest {
        val now = NOW
        blockDao.upsert(block("pool-1", now))
        configDao.upsert(config("pool-1", now, enableAutoCleanup = false))
        repeat(5) { index -> nodeDao.upsert(node("node-$index", "pool-1", timestamp = index.toLong())) }

        val auto = repository.autoCleanup("pool-1", now)
        assertEquals(CleanupSkipReason.AUTO_CLEANUP_DISABLED, auto.skippedReason)
        assertEquals(5, nodeDao.countActive("pool-1"))

        val preview = repository.cleanupNow("pool-1", preview = true, now = now)
        assertEquals(3, preview.candidates.size)
        assertEquals(0, preview.deletedCount)
        assertEquals(5, nodeDao.countActive("pool-1"))

        val executed = repository.cleanupNow("pool-1", preview = false, now = now)
        assertEquals(3, executed.deletedCount)
        assertEquals(2, nodeDao.countActive("pool-1"))
    }

    @Test
    fun enabledCleanupDeletesAndPinnedPoolIsSkipped() = runTest {
        val now = NOW
        blockDao.upsert(block("pool-2", now))
        configDao.upsert(config("pool-2", now, enableAutoCleanup = true))
        repeat(5) { index -> nodeDao.upsert(node("node-$index", "pool-2", timestamp = index.toLong())) }

        val auto = repository.autoCleanup("pool-2", now)
        assertEquals(3, auto.deletedCount)
        assertEquals(2, nodeDao.countActive("pool-2"))

        blockDao.setPinned("pool-2", true, now)
        val pinned = repository.cleanupNow("pool-2", preview = false, now = now)
        assertEquals(CleanupSkipReason.POOL_PINNED, pinned.skippedReason)
    }

    @Test
    fun purgeExpiredHardDeletesOnlyOldRecycleBinItems() = runTest {
        val now = NOW
        blockDao.upsert(block("pool-3", now))
        configDao.upsert(config("pool-3", now))
        nodeDao.upsert(node("node-1", "pool-3", timestamp = 1L))
        blockDao.softDelete(
            poolId = "pool-3",
            deletedAt = now - TimeUnit.DAYS.toMillis(8),
            updatedAt = now,
        )

        val purged = repository.purgeExpired(now)
        assertEquals(1, purged)
        assertNull(blockDao.getById("pool-3"))
        assertEquals(0, nodeDao.getAllByPool("pool-3").size)
    }

    @Test
    fun runtimeConfigDefaultsAreReturnedWhenRowMissing() = runTest {
        blockDao.upsert(block("pool-4", NOW))
        val config = repository.getConfig("pool-4")
        assertNotNull(config)
        assertEquals(false, config.enableAutoCleanup)
        assertEquals(2, config.cleanupThresholdCount)
        assertEquals(7, config.deletedRetentionDays)
    }

    private fun block(id: String, now: Long): MemoryPoolBlockEntity = MemoryPoolBlockEntity(
        poolId = id,
        title = "对话$id",
        createdAt = now,
        updatedAt = now,
    )

    private fun node(id: String, poolId: String, timestamp: Long): MemoryHeapNodeEntity =
        MemoryHeapNodeEntity(
            nodeId = id,
            poolId = poolId,
            role = MessageRole.USER,
            content = "内容$id",
            timestamp = timestamp,
        )

    private fun config(
        poolId: String,
        now: Long,
        enableAutoCleanup: Boolean = false,
    ): MemoryPoolConfigEntity = MemoryPoolConfigEntity(
        poolId = poolId,
        updatedAt = now,
        enableAutoCleanup = enableAutoCleanup,
        cleanupThresholdCount = 2,
        cleanupProtectionHours = 24,
        deletedRetentionDays = 7,
    )

    private companion object {
        const val NOW = 10_000_000_000L
    }
}
