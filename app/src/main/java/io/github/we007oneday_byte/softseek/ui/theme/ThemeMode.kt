package io.github.we007oneday_byte.softseek.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * P1 临时内存态。P4-07 接入 DataStore 后替换为持久化设置。
 */
object ThemeManager {
    var mode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    fun cycle() {
        mode = when (mode) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
    }
}
