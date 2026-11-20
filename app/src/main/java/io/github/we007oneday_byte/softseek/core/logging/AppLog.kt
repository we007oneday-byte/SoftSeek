package io.github.we007oneday_byte.softseek.core.logging

import android.content.Context
import ch.qos.logback.classic.Level
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.slf4j.Marker
import org.slf4j.MarkerFactory
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * 应用统一日志入口。业务代码不直接使用 android.util.Log 或 slf4j，
 * 而是通过这里强制带上模块编号、日志类型与可选指标字段。
 */
object AppLog {
    private const val CRASH_MARKER_NAME = "CRASH"

    private val logger by lazy { LoggerFactory.getLogger("SoftSeek") }
    private val crashMarker: Marker by lazy { MarkerFactory.getMarker(CRASH_MARKER_NAME) }
    private val currentModule = AtomicReference(LogModule.APP)
    private val installed = AtomicBoolean(false)

    @Volatile
    private var initError: Throwable? = null

    fun install(context: Context, appVersion: String) {
        if (!installed.compareAndSet(false, true)) return
        SoftSeekLogEnvironment.init(appVersion)
        runCatching { LogbackConfigurator.configure(context) }
            .onFailure { error ->
                initError = error
                // 日志系统自身异常不能反过来让应用启动崩溃，保留系统 Log 兜底。
                android.util.Log.e("SoftSeek", "logback 初始化失败", error)
            }
        installCrashHandler()
        AnrWatchdog.start()
        info(LogModule.APP, "日志系统初始化", metric = "log_init", value = 1, unit = "次")
    }

    fun setCurrentModule(module: LogModule) {
        currentModule.set(module)
    }

    fun currentModule(): LogModule = currentModule.get()

    fun initFailure(): Throwable? = initError

    fun info(
        module: LogModule,
        message: String,
        metric: String? = null,
        value: Any? = null,
        unit: String? = null,
    ) {
        write(Level.INFO, "INFO", module, message, null, metric, value, unit)
    }

    fun metric(
        module: LogModule,
        name: String,
        value: Any?,
        unit: String?,
        message: String = name,
    ) {
        write(Level.INFO, "METRIC", module, message, null, name, value, unit)
    }

    fun error(
        module: LogModule,
        message: String,
        throwable: Throwable? = null,
        metric: String? = null,
        value: Any? = null,
        unit: String? = null,
    ) {
        write(Level.ERROR, "ERROR", module, message, throwable, metric, value, unit)
    }

    fun crash(
        module: LogModule,
        throwable: Throwable,
        message: String = "未捕获异常",
    ) {
        val crashId = "crash-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(8)}"
        write(
            level = Level.ERROR,
            type = "CRASH",
            module = module,
            message = "$message: ${throwable.message.orEmpty()}",
            throwable = throwable,
            crashId = crashId,
        )
    }

    fun anr(
        module: LogModule,
        message: String,
    ) {
        val crashId = "anr-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(8)}"
        write(
            level = Level.ERROR,
            type = "ANR",
            module = module,
            message = message,
            crashId = crashId,
        )
    }

    private fun write(
        level: Level,
        type: String,
        module: LogModule,
        message: String,
        throwable: Throwable? = null,
        metric: String? = null,
        value: Any? = null,
        unit: String? = null,
        crashId: String? = null,
    ) {
        MDC.put("module", module.code)
        MDC.put("type", type)
        metric?.let { MDC.put("metric", it) }
        value?.let { MDC.put("value", it.toString()) }
        unit?.let { MDC.put("unit", it) }
        crashId?.let { MDC.put("crashId", it) }
        try {
            when {
                type == "CRASH" || type == "ANR" -> {
                    if (throwable != null) {
                        logger.error(crashMarker, message, throwable)
                    } else {
                        logger.error(crashMarker, message)
                    }
                }
                level == Level.ERROR -> {
                    if (throwable != null) {
                        logger.error(message, throwable)
                    } else {
                        logger.error(message)
                    }
                }
                else -> logger.info(message)
            }
        } finally {
            MDC.remove("module")
            MDC.remove("type")
            MDC.remove("metric")
            MDC.remove("value")
            MDC.remove("unit")
            MDC.remove("crashId")
        }
    }

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                crash(
                    module = currentModule.get(),
                    throwable = throwable,
                    message = "未捕获异常 thread=${thread.name}",
                )
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
