package io.github.we007oneday_byte.softseek.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.we007oneday_byte.softseek.data.repository.DefaultChatRepository
import io.github.we007oneday_byte.softseek.data.repository.RoomConversationRepository
import io.github.we007oneday_byte.softseek.data.repository.RoomMemoryPoolRepository
import io.github.we007oneday_byte.softseek.data.repository.RoomUsageRepository
import io.github.we007oneday_byte.softseek.domain.repository.ChatRepository
import io.github.we007oneday_byte.softseek.domain.repository.ConversationRepository
import io.github.we007oneday_byte.softseek.domain.repository.MemoryPoolRepository
import io.github.we007oneday_byte.softseek.domain.repository.UsageRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindConversationRepository(impl: RoomConversationRepository): ConversationRepository

    @Binds
    @Singleton
    abstract fun bindMemoryPoolRepository(impl: RoomMemoryPoolRepository): MemoryPoolRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: DefaultChatRepository): ChatRepository

    @Binds
    @Singleton
    abstract fun bindUsageRepository(impl: RoomUsageRepository): UsageRepository
}
