package io.github.we007oneday_byte.softseek.core.asr

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Vosk 模型生命周期：下载（支持 Range 续传）、大小/SHA-256 校验、安全解压、状态广播。
 * 模型目录只在完整性校验通过后才替换，失败时保留 .part 供下次续传。
 */
@Singleton
class VoskModelManager
    @Inject
    constructor(
        @ApplicationContext context: Context,
        private val okHttpClient: OkHttpClient,
    ) {
        private val store = VoskModelStore(File(context.filesDir, "models"))
        private val spec = VoskModelSpec.SMALL_CN
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val mutex = Mutex()

        private val _state = MutableStateFlow(store.inspect(spec))
        val state: StateFlow<VoskModelState> = _state.asStateFlow()

        private var installJob: Job? = null
        private var currentCall: Call? = null

        fun refresh() {
            if (installJob?.isActive == true) return
            _state.value = store.inspect(spec)
        }

        fun install() {
            if (installJob?.isActive == true) return
            installJob = scope.launch {
                mutex.withLock {
                    installInternal()
                }
            }
        }

        fun cancelInstall() {
            currentCall?.cancel()
            currentCall = null
            installJob?.cancel()
            installJob = null
            _state.value = store.inspect(spec)
        }

        suspend fun ensureInstalled(): Result<File> =
            mutex.withLock {
                if (store.isInstalled(spec)) {
                    Result.success(store.modelDir(spec))
                } else {
                    installInternal()
                }
            }

        private suspend fun installInternal(): Result<File> {
            try {
                if (store.isInstalled(spec)) {
                    _state.value = VoskModelState.Installed(store.modelDir(spec).absolutePath)
                    return Result.success(store.modelDir(spec))
                }

                val archive = store.archivePart(spec)
                if (archive.exists() && archive.length() > spec.archiveBytes) {
                    archive.delete()
                }
                download(archive)

                _state.value = VoskModelState.Verifying
                val verification = store.verifyArchive(archive, spec)
                if (verification.isFailure) {
                    archive.delete()
                    throw verification.exceptionOrNull()
                        ?: IllegalStateException("模型校验失败")
                }

                _state.value = VoskModelState.Extracting
                val extracted = store.extractArchive(archive, spec)
                if (extracted.isFailure) {
                    throw extracted.exceptionOrNull() ?: IllegalStateException("模型解压失败")
                }
                val modelDir = extracted.getOrThrow()
                archive.delete()
                _state.value = VoskModelState.Installed(modelDir.absolutePath)
                return Result.success(modelDir)
            } catch (cancelled: CancellationException) {
                _state.value = store.inspect(spec)
                throw cancelled
            } catch (error: Exception) {
                _state.value = VoskModelState.Failed(
                    message = error.message ?: "模型安装失败",
                    retryable = true,
                )
                return Result.failure(error)
            }
        }

        private suspend fun download(archive: File) {
            store.ensureModelsDir().getOrElse { throw it }
            val existing = if (archive.isFile) archive.length() else 0L
            if (existing == spec.archiveBytes) {
                _state.value = VoskModelState.Downloading(existing, spec.archiveBytes)
                return
            }

            val request = Request.Builder()
                .url(spec.downloadUrl)
                .apply {
                    if (existing > 0L) header("Range", "bytes=$existing-")
                }
                .build()
            val client = okHttpClient.newBuilder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build()

            _state.value = VoskModelState.Downloading(existing, spec.archiveBytes)
            currentCall = client.newCall(request)
            try {
                currentCall?.execute()?.use { response ->
                    if (!response.isSuccessful) {
                        if (response.code == 416 && existing == spec.archiveBytes) return
                        throw IOException("模型下载失败：HTTP ${response.code}")
                    }
                    val body = response.body ?: throw IOException("模型下载响应为空")
                    val append = response.code == 206 && existing > 0L
                    if (!append) archive.delete()
                    FileOutputStream(archive, append).use { output ->
                        var downloaded = if (append) existing else 0L
                        body.byteStream().use { input ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                downloaded += read
                                if (downloaded > spec.archiveBytes) {
                                    throw IOException("模型下载超出预期大小")
                                }
                                _state.value = VoskModelState.Downloading(
                                    downloadedBytes = downloaded,
                                    totalBytes = spec.archiveBytes,
                                )
                            }
                        }
                    }
                }
            } finally {
                currentCall = null
            }
        }

        companion object {
            private const val BUFFER_SIZE = 8 * 1024
        }
    }
