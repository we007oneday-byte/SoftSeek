package io.github.we007oneday_byte.softseek.core.tts

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface TtsEngine {
    val events: Flow<TtsEvent>
    val ready: StateFlow<Boolean>

    fun speak(
        text: String,
        utteranceId: String,
        queueMode: TtsQueueMode = TtsQueueMode.FLUSH,
    ): Result<Unit>

    fun stop()

    fun setRate(rate: Float)

    fun setPitch(pitch: Float)

    fun release()
}

enum class TtsQueueMode {
    /** 清空当前队列后播报，用于新回复或设置页试听。 */
    FLUSH,

    /** 追加到当前队列，用于同一回复的后续分句。 */
    ADD,
}

sealed interface TtsEvent {
    data class Started(val utteranceId: String) : TtsEvent

    data class Done(val utteranceId: String) : TtsEvent

    data class Error(
        val utteranceId: String?,
        val message: String,
    ) : TtsEvent
}
