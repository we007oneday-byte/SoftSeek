package io.github.we007oneday_byte.softseek.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.we007oneday_byte.softseek.domain.model.MessageRole
import io.github.we007oneday_byte.softseek.domain.model.MessageStatus

@Entity(
    tableName = "memory_heap_nodes",
    foreignKeys = [
        ForeignKey(
            entity = MemoryPoolBlockEntity::class,
            parentColumns = ["poolId"],
            childColumns = ["poolId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["poolId", "timestamp"]),
        Index(value = ["parentNodeId"]),
    ],
)
data class MemoryHeapNodeEntity(
    @PrimaryKey val nodeId: String,
    val poolId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long,
    val parentNodeId: String? = null,
    val reasoningContent: String? = null,
    val reasoningTokens: Int = 0,
    val modelName: String? = null,
    val status: MessageStatus = MessageStatus.COMPLETE,
    val finishReason: String? = null,
    val errorMessage: String? = null,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
)
