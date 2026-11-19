package io.github.we007oneday_byte.softseek.domain.repository

interface UsageRepository {
    suspend fun totalTokens(): Long

    suspend fun tokensSince(since: Long): Long

    suspend fun tokensBetween(start: Long, end: Long): Long
}
