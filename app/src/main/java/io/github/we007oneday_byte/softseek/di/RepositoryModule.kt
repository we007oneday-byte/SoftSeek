package io.github.we007oneday_byte.softseek.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.we007oneday_byte.softseek.data.repository.RoomConversationRepository
import io.github.we007oneday_byte.softseek.data.repository.RoomMemoryPoolRepository
import io.github.we007oneday_byte.softseek.domain.repository.ConversationRepository
import io.github.we007oneday_byte.softseek.domain.repository.MemoryPoolRepository
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
}
