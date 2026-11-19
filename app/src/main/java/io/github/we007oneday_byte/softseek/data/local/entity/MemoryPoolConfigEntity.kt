package io.github.we007oneday_byte.softseek.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import io.github.we007oneday_byte.softseek.domain.model.ImageDetail

@Entity(
    tableName = "memory_pool_config",
    foreignKeys = [
        ForeignKey(
            entity = MemoryPoolBlockEntity::class,
            parentColumns = ["poolId"],
            childColumns = ["poolId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class MemoryPoolConfigEntity(
    @PrimaryKey val poolId: String,
    val updatedAt: Long,
    val maxInputTokens: Int = 32_768,
    val enableAutoCleanup: Boolean = false,
    val cleanupThresholdCount: Int = 2,
    val cleanupProtectionHours: Int = 24,
    val deletedRetentionDays: Int = 7,
    val tokenEstimateRatio: Double = 1.5,
    val callAutoDowngrade: Boolean = true,
    val imageDetail: ImageDetail = ImageDetail.AUTO,
)
