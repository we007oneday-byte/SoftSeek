package io.github.we007oneday_byte.softseek.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.we007oneday_byte.softseek.core.tts.SystemTtsEngine
import io.github.we007oneday_byte.softseek.core.tts.TtsEngine
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceModule {
    @Binds
    @Singleton
    abstract fun bindTtsEngine(engine: SystemTtsEngine): TtsEngine
}
