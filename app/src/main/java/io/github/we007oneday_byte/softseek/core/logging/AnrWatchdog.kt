package io.github.we007oneday_byte.softseek.core.logging

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * logback 能捕获 Java 崩溃，但系统 ANR 通常由系统进程处理，应用进程拿不到
 * 及时回调。这里用主线程心跳做本地兜底：5 秒未响应就写一条 ANR 级别日志，
 * 并附带主线程堆栈，方便在日志页优先看到卡顿线索。
 */
object AnrWatchdog {
    private const val THRESHOLD_MS = 5_000L
    private const val COOLDOWN_MS = 30_000L

    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        val mainLooper = Looper.getMainLooper()
        val handler = Handler(mainLooper)
        val lastHeartbeat = AtomicLong(SystemClock.uptimeMillis())
        Thread({
            var lastReportedAt = 0L
            while (true) {
                handler.post { lastHeartbeat.set(SystemClock.uptimeMillis()) }
                Thread.sleep(1_000L)
                val now = SystemClock.uptimeMillis()
                val delayed = now - lastHeartbeat.get()
                if (delayed >= THRESHOLD_MS && now - lastReportedAt >= COOLDOWN_MS) {
                    lastReportedAt = now
                    val stack = mainLooper.thread.stackTrace
                        .take(24)
                        .joinToString("\n") { it.toString() }
                    AppLog.anr(
                        AppLog.currentModule(),
                        "主线程疑似卡顿 ${delayed}ms\n$stack",
                    )
                }
            }
        }, "softseek-anr-watchdog").apply {
            isDaemon = true
            start()
        }
    }
}
