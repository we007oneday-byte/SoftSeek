package io.github.we007oneday_byte.softseek.data.cleanup

import io.github.we007oneday_byte.softseek.data.local.dao.MemoryHeapNodeDao
import io.github.we007oneday_byte.softseek.data.local.dao.MemoryPoolBlockDao
import io.github.we007oneday_byte.softseek.data.local.dao.MemoryPoolConfigDao
import io.github.we007oneday_byte.softseek.data.repository.toDomain
import io.github.we007oneday_byte.softseek.domain.model.CleanupCandidate
import io.github.we007oneday_byte.softseek.domain.model.CleanupResult
import io.github.we007oneday_byte.softseek.domain.model.CleanupSkipReason
import io.github.we007oneday_byte.softseek.domain.model.MemoryPoolConfig
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CleanupEngine
    @Inject
    constructor(
        private val blockDao: MemoryPoolBlockDao,
        private val nodeDao: MemoryHeapNodeDao,
        private val configDao: MemoryPoolConfigDao,
    ) {
        suspend fun cleanupNow(
            poolId: String,
            preview: Boolean,
            now: Long = System.currentTimeMillis(),
        ): CleanupResult {
            val block = blockDao.getById(poolId)
                ?: return emptyResult(poolId, preview, CleanupSkipReason.POOL_NOT_FOUND)
            if (block.isPinned) {
                return emptyResult(poolId, preview, CleanupSkipReason.POOL_PINNED)
            }
            val config = configDao.get(poolId)?.toDomain() ?: MemoryPoolConfig.defaults(poolId, now)
            val protectionCutoff = now - TimeUnit.HOURS.toMillis(config.cleanupProtectionHours.toLong())
            val keepCount = config.cleanupThresholdCount.coerceAtLeast(0)
            val candidates = nodeDao.getCleanupCandidates(poolId, keepCount, protectionCutoff)
            if (candidates.isEmpty()) {
                return emptyResult(poolId, preview, CleanupSkipReason.NOTHING_TO_CLEAN)
            }
            val mapped = candidates.map {
                CleanupCandidate(
                    nodeId = it.nodeId,
                    role = it.role,
                    preview = it.content.take(PREVIEW_LENGTH).ifBlank { EMPTY_PREVIEW },
                    timestamp = it.timestamp,
                )
            }
            val deletedCount = if (preview) {
                0
            } else {
                nodeDao.softDeleteByIds(mapped.map { it.nodeId }, now)
            }
            return CleanupResult(
                poolId = poolId,
                preview = preview,
                candidates = mapped,
                deletedCount = deletedCount,
            )
        }

        suspend fun autoCleanup(
            poolId: String,
            now: Long = System.currentTimeMillis(),
        ): CleanupResult {
            val config = configDao.get(poolId)?.toDomain() ?: MemoryPoolConfig.defaults(poolId, now)
            if (!config.enableAutoCleanup) {
                return emptyResult(poolId, preview = false, CleanupSkipReason.AUTO_CLEANUP_DISABLED)
            }
            return cleanupNow(poolId, preview = false, now = now)
        }

        private fun emptyResult(
            poolId: String,
            preview: Boolean,
            reason: CleanupSkipReason,
        ): CleanupResult = CleanupResult(
            poolId = poolId,
            preview = preview,
            candidates = emptyList(),
            deletedCount = 0,
            skippedReason = reason,
        )

        private companion object {
            const val PREVIEW_LENGTH = 80
            const val EMPTY_PREVIEW = "[空内容]"
        }
    }
