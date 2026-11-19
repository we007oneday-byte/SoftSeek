package io.github.we007oneday_byte.softseek.core.asr

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
        @ApplicationContext private val context: Context,
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

        /**
         * 从系统文件选择器导入官方 ZIP，不依赖网络。
         * 导入包仍然执行大小 + SHA-256 校验，避免装错模型。
         */
        fun installFromUri(uri: Uri) {
            if (installJob?.isActive == true) return
            installJob = scope.launch {
                mutex.withLock {
                    importInternal(uri)
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
                store.installedDir(spec)?.let { return@withLock Result.success(it) }
                installInternal()
            }

        private suspend fun installInternal(): Result<File> {
            try {
                store.installedDir(spec)?.let { installed ->
                    _state.value = VoskModelState.Installed(installed.absolutePath)
                    return Result.success(installed)
                }

                val archive = store.archivePart(spec)
                if (archive.exists() && archive.length() > spec.archiveBytes) {
                    archive.delete()
                }
                downloadWithRetry(archive)

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
                    message = if (error is IOException) {
                        "模型下载网络中断：${error.message ?: "连接被中断"}。可重试，或从本地 ZIP 导入。"
                    } else {
                        error.message ?: "模型安装失败"
                    },
                    retryable = true,
                )
                return Result.failure(error)
            }
        }

        /**
         * 把用户通过系统文件选择器选中的官方 ZIP 复制到私有缓存，
         * 再走与下载完全一致的 size + SHA-256 校验和安全解压流程。
         */
        private suspend fun importInternal(uri: Uri): Result<File> {
            val temp = File(context.cacheDir, "vosk-import-${System.currentTimeMillis()}.zip")
            try {
                store.installedDir(spec)?.let { installed ->
                    _state.value = VoskModelState.Installed(installed.absolutePath)
                    return Result.success(installed)
                }
                context.cacheDir.mkdirs()
                _state.value = VoskModelState.Verifying
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("无法读取所选 ZIP 文件")
                input.use { source ->
                    temp.outputStream().use { target ->
                        source.copyTo(target, BUFFER_SIZE)
                    }
                }
                val verification = store.verifyArchive(temp, spec)
                if (verification.isFailure) {
                    throw verification.exceptionOrNull()
                        ?: IllegalStateException("模型校验失败")
                }
                _state.value = VoskModelState.Extracting
                val extracted = store.extractArchive(temp, spec)
                    .getOrElse { throw it }
                _state.value = VoskModelState.Installed(extracted.absolutePath)
                return Result.success(extracted)
            } catch (cancelled: CancellationException) {
                _state.value = store.inspect(spec)
                throw cancelled
            } catch (error: Exception) {
                _state.value = VoskModelState.Failed(
                    message = "本地模型导入失败：${error.message ?: "文件无法读取"}",
                    retryable = false,
                )
                return Result.failure(error)
            } finally {
                temp.delete()
            }
        }

        private suspend fun downloadWithRetry(archive: File) {
            var lastError: IOException? = null
            for (attempt in 1..MAX_DOWNLOAD_ATTEMPTS) {
                try {
                    download(archive)
                    return
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: IOException) {
                    lastError = error
                    if (attempt < MAX_DOWNLOAD_ATTEMPTS) {
                        delay(RETRY_DELAY_BASE_MILLIS * attempt)
                    }
                }
            }
            throw lastError ?: IOException("模型下载失败")
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
                .header("User-Agent", "SoftSeek-Android")
                .header("Accept", "*/*")
                .apply {
                    if (existing > 0L) header("Range", "bytes=$existing-")
                }
                .build()
            val client = okHttpClient.newBuilder()
                .readTimeout(DOWNLOAD_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
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
            private const val MAX_DOWNLOAD_ATTEMPTS = 3
            private const val RETRY_DELAY_BASE_MILLIS = 1_500L
            private const val DOWNLOAD_READ_TIMEOUT_SECONDS = 60L
        }
    }
