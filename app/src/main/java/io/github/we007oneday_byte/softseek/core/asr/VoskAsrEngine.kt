package io.github.we007oneday_byte.softseek.core.asr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File

/**
 * Vosk 流式引擎。Recognizer.acceptWaveForm 返回 true 时即检测到句尾（VAD），
 * 立即产出 final 并等待下一句；否则产出 deduplicated partial。
 */
class VoskAsrEngine(
    private val modelDirProvider: () -> File?,
) : AsrEngine {
    private val _events = MutableSharedFlow<AsrEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val events: SharedFlow<AsrEvent> = _events.asSharedFlow()

    private val loadMutex = Mutex()

    @Volatile
    private var model: Model? = null

    @Volatile
    private var recognizer: Recognizer? = null

    @Volatile
    private var lastPartial: String? = null

    override suspend fun start(): Result<Unit> = withContext(Dispatchers.IO) {
        loadMutex.withLock {
            if (recognizer != null) return@withLock Result.success(Unit)
            try {
                val modelDir = modelDirProvider()
                    ?: return@withLock fail("离线识别模型尚未安装")
                if (!modelDir.isDirectory) {
                    return@withLock fail("离线识别模型目录不存在")
                }
                val newModel = Model(modelDir.absolutePath)
                model = newModel
                val newRecognizer = Recognizer(newModel, SAMPLE_RATE.toFloat())
                recognizer = newRecognizer
                lastPartial = null
                Result.success(Unit)
            } catch (error: Exception) {
                closeRecognizer()
                model?.close()
                model = null
                fail(error.message ?: "Vosk 模型加载失败", error)
            }
        }
    }

    override fun acceptAudio(
        samples: ShortArray,
        offset: Int,
        length: Int,
    ) {
        val current = recognizer ?: return
        if (length <= 0 || offset < 0 || offset + length > samples.size) return
        try {
            val data = if (offset == 0 && length == samples.size) {
                samples
            } else {
                samples.copyOfRange(offset, offset + length)
            }
            val endOfSentence = current.acceptWaveForm(data, data.size)
            if (endOfSentence) {
                emitFinal(VoskResultParser.text(current.result))
            } else {
                emitPartial(VoskResultParser.text(current.partialResult))
            }
        } catch (error: Exception) {
            emitError(error.message ?: "Vosk 识别失败", error)
        }
    }

    override fun finish() {
        val current = recognizer ?: return
        try {
            emitFinal(VoskResultParser.text(current.finalResult))
            current.reset()
            lastPartial = null
        } catch (error: Exception) {
            emitError(error.message ?: "Vosk 结束识别失败", error)
        }
    }

    override fun cancel() {
        try {
            recognizer?.reset()
        } catch (_: Exception) {
            // reset 失败不影响下一次 start/release；错误由后续识别暴露。
        }
        lastPartial = null
    }

    override fun release() {
        closeRecognizer()
        model?.close()
        model = null
        lastPartial = null
    }

    private fun emitPartial(text: String?) {
        if (text == null || text == lastPartial) return
        lastPartial = text
        _events.tryEmit(AsrEvent.Partial(text))
    }

    private fun emitFinal(text: String?) {
        lastPartial = null
        if (text.isNullOrBlank()) return
        _events.tryEmit(AsrEvent.Final(text))
    }

    private fun emitError(
        message: String,
        cause: Throwable? = null,
    ) {
        _events.tryEmit(AsrEvent.Error(message, cause))
    }

    private fun fail(
        message: String,
        cause: Throwable? = null,
    ): Result<Unit> {
        emitError(message, cause)
        return Result.failure(cause ?: IllegalStateException(message))
    }

    private fun closeRecognizer() {
        try {
            recognizer?.close()
        } catch (_: Exception) {
            // 原生库关闭失败时仍要继续释放 Model，避免泄漏。
        }
        recognizer = null
    }

    companion object {
        const val SAMPLE_RATE = 16_000
    }
}
