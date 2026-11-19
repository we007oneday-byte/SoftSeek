package io.github.we007oneday_byte.softseek.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.we007oneday_byte.softseek.data.local.entity.MemoryHeapNodeEntity
import io.github.we007oneday_byte.softseek.data.local.entity.MemoryHeapNodeWithAttachments
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryHeapNodeDao {
    @Transaction
    @Query("SELECT * FROM memory_heap_nodes WHERE poolId = :poolId AND isDeleted = 0 ORDER BY timestamp ASC")
    fun observeWithAttachments(poolId: String): Flow<List<MemoryHeapNodeWithAttachments>>

    @Transaction
    @Query("SELECT * FROM memory_heap_nodes WHERE poolId = :poolId AND isDeleted = 0 ORDER BY timestamp ASC")
    suspend fun getWithAttachments(poolId: String): List<MemoryHeapNodeWithAttachments>

    @Query("SELECT * FROM memory_heap_nodes WHERE poolId = :poolId ORDER BY timestamp ASC")
    suspend fun getAllByPool(poolId: String): List<MemoryHeapNodeEntity>

    @Query("SELECT COUNT(*) FROM memory_heap_nodes WHERE poolId = :poolId AND isDeleted = 0")
    suspend fun countActive(poolId: String): Int

    @Query(
        """
        SELECT * FROM memory_heap_nodes
        WHERE poolId = :poolId AND isDeleted = 0 AND timestamp < :protectionCutoff
        ORDER BY timestamp DESC
        LIMIT -1 OFFSET :keepCount
        """,
    )
    suspend fun getCleanupCandidates(
        poolId: String,
        keepCount: Int,
        protectionCutoff: Long,
    ): List<MemoryHeapNodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(node: MemoryHeapNodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(nodes: List<MemoryHeapNodeEntity>)

    @Query(
        """
        UPDATE memory_heap_nodes
        SET isDeleted = 1, deletedAt = :deletedAt
        WHERE nodeId IN (:nodeIds)
        """,
    )
    suspend fun softDeleteByIds(nodeIds: List<String>, deletedAt: Long): Int

    @Query("DELETE FROM memory_heap_nodes WHERE nodeId = :nodeId")
    suspend fun deleteById(nodeId: String): Int
}
