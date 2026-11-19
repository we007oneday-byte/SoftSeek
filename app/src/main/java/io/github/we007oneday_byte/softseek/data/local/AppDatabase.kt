package io.github.we007oneday_byte.softseek.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.we007oneday_byte.softseek.data.local.converter.Converters
import io.github.we007oneday_byte.softseek.data.local.dao.MemoryHeapNodeDao
import io.github.we007oneday_byte.softseek.data.local.dao.MemoryPoolBlockDao
import io.github.we007oneday_byte.softseek.data.local.dao.MemoryPoolConfigDao
import io.github.we007oneday_byte.softseek.data.local.dao.MessageAttachmentDao
import io.github.we007oneday_byte.softseek.data.local.entity.MemoryHeapNodeEntity
import io.github.we007oneday_byte.softseek.data.local.entity.MemoryPoolBlockEntity
import io.github.we007oneday_byte.softseek.data.local.entity.MemoryPoolConfigEntity
import io.github.we007oneday_byte.softseek.data.local.entity.MessageAttachmentEntity

@Database(
    entities = [
        MemoryPoolBlockEntity::class,
        MemoryHeapNodeEntity::class,
        MessageAttachmentEntity::class,
        MemoryPoolConfigEntity::class,
    ],
    version = AppDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoryPoolBlockDao(): MemoryPoolBlockDao

    abstract fun memoryHeapNodeDao(): MemoryHeapNodeDao

    abstract fun messageAttachmentDao(): MessageAttachmentDao

    abstract fun memoryPoolConfigDao(): MemoryPoolConfigDao

    companion object {
        const val NAME = "softseek.db"
        const val VERSION = 1
    }
}
