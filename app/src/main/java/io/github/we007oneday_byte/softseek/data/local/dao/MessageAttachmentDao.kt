package io.github.we007oneday_byte.softseek.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.we007oneday_byte.softseek.data.local.entity.MessageAttachmentEntity

@Dao
interface MessageAttachmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(attachment: MessageAttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(attachments: List<MessageAttachmentEntity>)

    @Query("SELECT * FROM message_attachments WHERE nodeId = :nodeId ORDER BY sortOrder ASC")
    suspend fun getByNode(nodeId: String): List<MessageAttachmentEntity>

    @Query("DELETE FROM message_attachments WHERE nodeId = :nodeId")
    suspend fun deleteByNode(nodeId: String): Int
}
