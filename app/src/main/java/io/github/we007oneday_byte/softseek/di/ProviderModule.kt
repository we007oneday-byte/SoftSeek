package io.github.we007oneday_byte.softseek.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.we007oneday_byte.softseek.data.remote.provider.DeepSeekProvider
import io.github.we007oneday_byte.softseek.domain.provider.AiProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProviderModule {
    @Binds
    @Singleton
    abstract fun bindAiProvider(provider: DeepSeekProvider): AiProvider
}
