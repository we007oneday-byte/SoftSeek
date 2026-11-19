package io.github.we007oneday_byte.softseek.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.we007oneday_byte.softseek.data.local.AppDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .build()

    @Provides
    fun provideMemoryPoolBlockDao(database: AppDatabase) = database.memoryPoolBlockDao()

    @Provides
    fun provideMemoryHeapNodeDao(database: AppDatabase) = database.memoryHeapNodeDao()

    @Provides
    fun provideMessageAttachmentDao(database: AppDatabase) = database.messageAttachmentDao()

    @Provides
    fun provideMemoryPoolConfigDao(database: AppDatabase) = database.memoryPoolConfigDao()
}
