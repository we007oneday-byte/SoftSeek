package io.github.we007oneday_byte.softseek.core.logging

/**
 * 业务模块唯一编号。日志、指标、崩溃、ANR 都必须带上这里的 code，
 * 便于在本地日志页按模块筛选，也便于导出后做问题定位。
 */
enum class LogModule(
    val code: String,
    val label: String,
) {
    APP("M00", "应用"),
    CHAT("M01", "聊天"),
    CALL("M02", "AI 通话"),
    SETTINGS("M03", "设置"),
    THEME("M04", "视觉主题"),
    NETWORK("M05", "网络"),
    STORAGE("M06", "存储"),
    ASR("M07", "离线识别"),
    TTS("M08", "语音合成"),
    MODEL("M09", "模型"),
    CRASH("M10", "崩溃"),
    UI("M11", "界面"),
    LOG("M12", "日志"),
    ;

    companion object {
        fun fromCode(code: String): LogModule =
            entries.firstOrNull { it.code == code } ?: APP
    }
}
