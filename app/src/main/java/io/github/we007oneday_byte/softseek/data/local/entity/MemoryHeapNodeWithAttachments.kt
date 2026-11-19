package io.github.we007oneday_byte.softseek.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class MemoryHeapNodeWithAttachments(
    @Embedded val node: MemoryHeapNodeEntity,
    @Relation(parentColumn = "nodeId", entityColumn = "nodeId")
    val attachments: List<MessageAttachmentEntity>,
)
