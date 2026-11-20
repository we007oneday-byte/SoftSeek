package io.github.we007oneday_byte.softseek.core.logging

import android.content.Context
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.encoder.LayoutWrappingEncoder
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import ch.qos.logback.core.spi.FilterReply
import ch.qos.logback.core.util.FileSize
import org.slf4j.LoggerFactory
import java.io.File

/**
 * 用代码配置 logback，而不依赖 assets/logback.xml。
 * 这样可以明确控制三个独立 appender、7 天滚动、错误/崩溃不被常规日志冲刷，
 * 也能在 Robolectric/JVM 测试中直接验证落盘。
 */
object LogbackConfigurator {
    private const val MAX_HISTORY_DAYS = 7
    private const val INFO_CAP = "20MB"
    private const val ERROR_CAP = "12MB"
    private const val CRASH_CAP = "8MB"

    fun configure(context: Context) {
        val loggerContext = LoggerFactory.getILoggerFactory() as? LoggerContext ?: return
        loggerContext.reset()
        val logDir = File(context.filesDir, "logs").apply { mkdirs() }

        val infoAppender = createAppender(
            context = loggerContext,
            logDir = logDir,
            prefix = "info",
            filter = InfoLevelFilter(),
            totalSizeCap = INFO_CAP,
        )
        val errorAppender = createAppender(
            context = loggerContext,
            logDir = logDir,
            prefix = "error",
            filter = ErrorOnlyFilter(),
            totalSizeCap = ERROR_CAP,
        )
        val crashAppender = createAppender(
            context = loggerContext,
            logDir = logDir,
            prefix = "crash",
            filter = CrashMarkerFilter(),
            totalSizeCap = CRASH_CAP,
        )

        val root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
        root.level = Level.INFO
        root.addAppender(infoAppender)
        root.addAppender(errorAppender)
        root.addAppender(crashAppender)
    }

    private fun createAppender(
        context: LoggerContext,
        logDir: File,
        prefix: String,
        filter: Filter<ILoggingEvent>?,
        totalSizeCap: String,
    ): RollingFileAppender<ILoggingEvent> {
        val appender = RollingFileAppender<ILoggingEvent>().apply {
            this.context = context
            name = "${prefix.uppercase()}_FILE"
            file = File(logDir, "softseek-$prefix.log").absolutePath
            isAppend = true
            isImmediateFlush = true
        }

        val layout = SoftSeekLogLayout().apply {
            this.context = context
            start()
        }
        appender.encoder = LayoutWrappingEncoder<ILoggingEvent>().apply {
            this.context = context
            this.layout = layout
            start()
        }

        filter?.let { appender.addFilter(it) }

        val rollingPolicy = TimeBasedRollingPolicy<ILoggingEvent>().apply {
            this.context = context
            fileNamePattern = File(logDir, "softseek-$prefix.%d{yyyy-MM-dd}.log").absolutePath
            maxHistory = MAX_HISTORY_DAYS
            setTotalSizeCap(FileSize.valueOf(totalSizeCap))
            isCleanHistoryOnStart = true
        }
        // policy 必须先挂到 appender，由 appender.start() 负责启动；
        // 提前 start() 会拿不到 parent 的 file 属性并抛 NPE。
        appender.rollingPolicy = rollingPolicy
        rollingPolicy.setParent(appender)
        appender.triggeringPolicy = rollingPolicy
        // policy 必须先绑定 parent appender，start 时才能读取 appender 的 file 属性。
        rollingPolicy.start()
        appender.start()
        return appender
    }

    private class InfoLevelFilter : Filter<ILoggingEvent>() {
        override fun decide(event: ILoggingEvent): FilterReply =
            if (event.level == Level.INFO) FilterReply.ACCEPT else FilterReply.DENY
    }

    private class ErrorOnlyFilter : Filter<ILoggingEvent>() {
        override fun decide(event: ILoggingEvent): FilterReply =
            if (event.level == Level.ERROR && !event.hasCrashMarker()) {
                FilterReply.ACCEPT
            } else {
                FilterReply.DENY
            }
    }

    private class CrashMarkerFilter : Filter<ILoggingEvent>() {
        override fun decide(event: ILoggingEvent): FilterReply =
            if (event.hasCrashMarker()) FilterReply.ACCEPT else FilterReply.DENY
    }

    private fun ILoggingEvent.hasCrashMarker(): Boolean =
        markers?.any { it.contains("CRASH") } == true
}
