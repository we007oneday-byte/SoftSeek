package io.github.we007oneday_byte.softseek.core.tts

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface TtsEngine {
    val events: Flow<TtsEvent>
    val ready: StateFlow<Boolean>

    fun speak(
        text: String,
        utteranceId: String,
    ): Result<Unit>

    fun stop()

    fun setRate(rate: Float)

    fun setPitch(pitch: Float)

    fun release()
}

sealed interface TtsEvent {
    data class Started(val utteranceId: String) : TtsEvent

    data class Done(val utteranceId: String) : TtsEvent

    data class Error(
        val utteranceId: String?,
        val message: String,
    ) : TtsEvent
}
