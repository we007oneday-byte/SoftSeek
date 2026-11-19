package io.github.we007oneday_byte.softseek.domain.repository

import io.github.we007oneday_byte.softseek.domain.model.CleanupResult
import io.github.we007oneday_byte.softseek.domain.model.MemoryPoolConfig
import kotlinx.coroutines.flow.Flow

interface MemoryPoolRepository {
    fun observeConfig(poolId: String): Flow<MemoryPoolConfig?>

    suspend fun getConfig(poolId: String): MemoryPoolConfig

    suspend fun upsertConfig(config: MemoryPoolConfig)

    suspend fun cleanupNow(
        poolId: String,
        preview: Boolean,
        now: Long = System.currentTimeMillis(),
    ): CleanupResult

    suspend fun autoCleanup(
        poolId: String,
        now: Long = System.currentTimeMillis(),
    ): CleanupResult

    suspend fun purgeExpired(now: Long = System.currentTimeMillis()): Int
}
