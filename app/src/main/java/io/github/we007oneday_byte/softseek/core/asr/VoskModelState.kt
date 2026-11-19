package io.github.we007oneday_byte.softseek.core.asr

sealed interface VoskModelState {
    data object NotInstalled : VoskModelState

    data class Downloading(
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : VoskModelState

    data object Verifying : VoskModelState

    data object Extracting : VoskModelState

    data class Installed(val modelDir: String) : VoskModelState

    data class Failed(
        val message: String,
        val retryable: Boolean = true,
    ) : VoskModelState
}
