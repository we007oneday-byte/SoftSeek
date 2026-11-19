package io.github.we007oneday_byte.softseek.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.we007oneday_byte.softseek.data.local.entity.MemoryPoolConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryPoolConfigDao {
    @Query("SELECT * FROM memory_pool_config WHERE poolId = :poolId")
    suspend fun get(poolId: String): MemoryPoolConfigEntity?

    @Query("SELECT * FROM memory_pool_config WHERE poolId = :poolId")
    fun observe(poolId: String): Flow<MemoryPoolConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: MemoryPoolConfigEntity)
}
