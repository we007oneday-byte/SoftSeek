package io.github.we007oneday_byte.softseek.ui.logging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import io.github.we007oneday_byte.softseek.core.logging.AppLog
import io.github.we007oneday_byte.softseek.core.logging.LogModule

/**
 * 页面级模块上下文：进入页面时设置当前业务模块，离开时恢复上一层。
 * 全局崩溃 handler 会读取 AppLog.currentModule()，因此崩溃日志也能落到具体页面模块。
 */
@Composable
fun LogModuleScope(
    module: LogModule,
    content: @Composable () -> Unit,
) {
    DisposableEffect(module) {
        val previous = AppLog.currentModule()
        AppLog.setCurrentModule(module)
        onDispose { AppLog.setCurrentModule(previous) }
    }
    content()
}
