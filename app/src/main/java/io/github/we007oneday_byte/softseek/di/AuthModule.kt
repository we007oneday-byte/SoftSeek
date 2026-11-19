package io.github.we007oneday_byte.softseek.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.we007oneday_byte.softseek.core.auth.InMemoryApiKeyStore
import io.github.we007oneday_byte.softseek.domain.auth.ApiKeyStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    @Singleton
    abstract fun bindApiKeyStore(store: InMemoryApiKeyStore): ApiKeyStore
}
