package io.github.we007oneday_byte.softseek.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.we007oneday_byte.softseek.domain.model.AttachmentType

@Entity(
    tableName = "message_attachments",
    foreignKeys = [
        ForeignKey(
            entity = MemoryHeapNodeEntity::class,
            parentColumns = ["nodeId"],
            childColumns = ["nodeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("nodeId")],
)
data class MessageAttachmentEntity(
    @PrimaryKey val attachmentId: String,
    val nodeId: String,
    val type: AttachmentType,
    val localPath: String,
    val mime: String,
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0,
    val sortOrder: Int = 0,
)
