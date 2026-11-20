package io.github.we007oneday_byte.softseek.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.we007oneday_byte.softseek.core.constant.AppConstants
import io.github.we007oneday_byte.softseek.domain.model.ReasoningEffort

@Entity(
    tableName = "memory_pool_blocks",
    indices = [
        Index(value = ["isDeleted", "updatedAt"]),
        Index(value = ["isPinned", "updatedAt"]),
    ],
)
data class MemoryPoolBlockEntity(
    @PrimaryKey val poolId: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val systemPrompt: String? = null,
    val modelName: String = AppConstants.DEFAULT_MODEL,
    val reasoningEffort: ReasoningEffort = ReasoningEffort.HIGH,
    val maxOutputTokens: Int = 8192,
    val tags: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val isDeleted: Boolean = false,
    val lastMessageAt: Long? = null,
    val lastMessagePreview: String? = null,
    val deletedAt: Long? = null,
)
