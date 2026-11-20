package io.github.we007oneday_byte.softseek.core.logging

import android.os.Build

/**
 * 设备与版本信息只初始化一次，由日志 layout 注入每一行，
 * 这样崩溃日志即使没有经过业务代码也能带上环境信息。
 */
object SoftSeekLogEnvironment {
    @Volatile
    var appVersion: String = "unknown"
        private set

    @Volatile
    var deviceModel: String = "unknown"
        private set

    @Volatile
    var osVersion: String = "unknown"
        private set

    fun init(versionName: String) {
        appVersion = versionName.ifBlank { "unknown" }
        deviceModel = listOf(Build.MANUFACTURER, Build.MODEL)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "unknown" }
        osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }
}
