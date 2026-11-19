package io.github.we007oneday_byte.softseek.core.asr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 只有模型校验/安装完成后才创建 Vosk 引擎；模型下载属于首次通话的前置步骤。
 */
@Singleton
class AsrEngineFactory
    @Inject
    constructor(
        private val modelManager: VoskModelManager,
    ) {
        suspend fun create(): Result<AsrEngine> = withContext(Dispatchers.IO) {
            val modelDir: File = modelManager.ensureInstalled().getOrElse { error ->
                return@withContext Result.failure(error)
            }
            Result.success(VoskAsrEngine { modelDir })
        }
    }
