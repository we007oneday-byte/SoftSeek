package io.github.we007oneday_byte.softseek.data.repository

import io.github.we007oneday_byte.softseek.data.cleanup.CleanupEngine
import io.github.we007oneday_byte.softseek.data.local.dao.MemoryPoolBlockDao
import io.github.we007oneday_byte.softseek.data.local.dao.MemoryPoolConfigDao
import io.github.we007oneday_byte.softseek.domain.model.CleanupResult
import io.github.we007oneday_byte.softseek.domain.model.MemoryPoolConfig
import io.github.we007oneday_byte.softseek.domain.repository.MemoryPoolRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomMemoryPoolRepository
    @Inject
    constructor(
        private val blockDao: MemoryPoolBlockDao,
        private val configDao: MemoryPoolConfigDao,
        private val cleanupEngine: CleanupEngine,
    ) : MemoryPoolRepository {
        override fun observeConfig(poolId: String): Flow<MemoryPoolConfig?> =
            configDao.observe(poolId).map { it?.toDomain() }

        override suspend fun getConfig(poolId: String): MemoryPoolConfig =
            configDao.get(poolId)?.toDomain()
                ?: MemoryPoolConfig.defaults(poolId, System.currentTimeMillis())

        override suspend fun upsertConfig(config: MemoryPoolConfig) {
            configDao.upsert(config.copy(updatedAt = System.currentTimeMillis()).toEntity())
        }

        override suspend fun cleanupNow(poolId: String, preview: Boolean, now: Long): CleanupResult =
            cleanupEngine.cleanupNow(poolId, preview, now)

        override suspend fun autoCleanup(poolId: String, now: Long): CleanupResult =
            cleanupEngine.autoCleanup(poolId, now)

        override suspend fun purgeExpired(now: Long): Int {
            var purged = 0
            val deletedBlocks = blockDao.getDeleted()
            for (block in deletedBlocks) {
                val deletedAt = block.deletedAt ?: continue
                val config = configDao.get(block.poolId)?.toDomain()
                    ?: MemoryPoolConfig.defaults(block.poolId, now)
                val retentionMillis = TimeUnit.DAYS.toMillis(config.deletedRetentionDays.toLong())
                if (deletedAt <= now - retentionMillis) {
                    purged += blockDao.deleteById(block.poolId)
                }
            }
            return purged
        }
    }
