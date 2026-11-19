package io.github.we007oneday_byte.softseek.core.asr

import kotlinx.coroutines.flow.Flow

/** 离线语音识别引擎的稳定边界；具体实现（Vosk/sherpa）不得泄漏到 UI 层。 */
interface AsrEngine {
    val events: Flow<AsrEvent>

    /** 加载模型并准备录音识别；失败时返回可展示的错误，不抛出。 */
    suspend fun start(): Result<Unit>

    /** 输入 16kHz、16bit、mono 的 PCM 样本。 */
    fun acceptAudio(
        samples: ShortArray,
        offset: Int = 0,
        length: Int = samples.size,
    )

    /** 结束当前一句话，触发 final 结果。 */
    fun finish()

    /** 丢弃当前句并停止识别，保留已加载模型。 */
    fun cancel()

    /** 释放模型和原生资源。 */
    fun release()
}

sealed interface AsrEvent {
    data class Partial(val text: String) : AsrEvent

    data class Final(val text: String) : AsrEvent

    data class Error(val message: String, val cause: Throwable? = null) : AsrEvent
}
