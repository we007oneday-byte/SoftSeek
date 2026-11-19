package io.github.we007oneday_byte.softseek.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.we007oneday_byte.softseek.data.local.entity.MemoryPoolBlockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryPoolBlockDao {
    @Query(
        """
        SELECT * FROM memory_pool_blocks
        WHERE isDeleted = 0
        ORDER BY isPinned DESC, COALESCE(lastMessageAt, updatedAt) DESC
        """,
    )
    fun observeActive(): Flow<List<MemoryPoolBlockEntity>>

    @Query("SELECT * FROM memory_pool_blocks WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun observeDeleted(): Flow<List<MemoryPoolBlockEntity>>

    @Query("SELECT * FROM memory_pool_blocks ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<MemoryPoolBlockEntity>>

    @Query("SELECT * FROM memory_pool_blocks WHERE poolId = :poolId")
    suspend fun getById(poolId: String): MemoryPoolBlockEntity?

    @Query("SELECT * FROM memory_pool_blocks WHERE isDeleted = 1 ORDER BY deletedAt ASC")
    suspend fun getDeleted(): List<MemoryPoolBlockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(block: MemoryPoolBlockEntity)

    @Query("UPDATE memory_pool_blocks SET title = :title, updatedAt = :updatedAt WHERE poolId = :poolId")
    suspend fun rename(poolId: String, title: String, updatedAt: Long): Int

    @Query("UPDATE memory_pool_blocks SET isPinned = :isPinned, updatedAt = :updatedAt WHERE poolId = :poolId")
    suspend fun setPinned(poolId: String, isPinned: Boolean, updatedAt: Long): Int

    @Query(
        """
        UPDATE memory_pool_blocks
        SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :updatedAt
        WHERE poolId = :poolId
        """,
    )
    suspend fun softDelete(poolId: String, deletedAt: Long, updatedAt: Long): Int

    @Query(
        """
        UPDATE memory_pool_blocks
        SET isDeleted = 0, deletedAt = NULL, updatedAt = :updatedAt
        WHERE poolId = :poolId
        """,
    )
    suspend fun restore(poolId: String, updatedAt: Long): Int

    @Query(
        """
        UPDATE memory_pool_blocks
        SET lastMessageAt = :lastMessageAt, lastMessagePreview = :preview, updatedAt = :updatedAt
        WHERE poolId = :poolId
        """,
    )
    suspend fun updateLastMessage(
        poolId: String,
        lastMessageAt: Long,
        preview: String?,
        updatedAt: Long,
    ): Int

    @Query("DELETE FROM memory_pool_blocks WHERE poolId = :poolId")
    suspend fun deleteById(poolId: String): Int
}
