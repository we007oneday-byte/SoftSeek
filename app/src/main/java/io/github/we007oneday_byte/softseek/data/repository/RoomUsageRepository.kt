package io.github.we007oneday_byte.softseek.data.repository

import io.github.we007oneday_byte.softseek.data.local.dao.MemoryHeapNodeDao
import io.github.we007oneday_byte.softseek.domain.repository.UsageRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomUsageRepository
    @Inject
    constructor(
        private val nodeDao: MemoryHeapNodeDao,
    ) : UsageRepository {
        override suspend fun totalTokens(): Long = nodeDao.sumTotalTokens()

        override suspend fun tokensSince(since: Long): Long = nodeDao.sumTokensSince(since)

        override suspend fun tokensBetween(start: Long, end: Long): Long =
            nodeDao.sumTokensBetween(start, end)
    }
