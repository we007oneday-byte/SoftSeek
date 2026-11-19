package io.github.we007oneday_byte.softseek.core.asr

/**
 * 模型规格是下载、校验和安装的唯一事实来源。
 * [sha256] 为空表示该版本尚未完成发布校验，管理器会拒绝安装，避免把未校验模型加载进原生库。
 */
data class VoskModelSpec(
    val id: String,
    val displayName: String,
    val downloadUrl: String,
    val archiveBytes: Long,
    val sha256: String,
    val rootDirName: String,
    val requiredPaths: List<String>,
) {
    companion object {
        const val SMALL_CN_ID = "vosk-small-cn-0.22"

        val SMALL_CN = VoskModelSpec(
            id = SMALL_CN_ID,
            displayName = "Vosk 中文小模型",
            downloadUrl = "https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip",
            archiveBytes = 43_898_754L,
            sha256 = "3af8b0e7e0f835ae9d414ce5df580237a3cfb08d586c9fbbb0f7ff29ad5b14ba",
            rootDirName = SMALL_CN_ID,
            requiredPaths = listOf(
                "am/final.mdl",
                "conf/mfcc.conf",
                "graph/HCLr.fst",
                "graph/Gr.fst",
            ),
        )
    }
}
