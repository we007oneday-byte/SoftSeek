package io.github.we007oneday_byte.softseek.ui.screens.logs

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.we007oneday_byte.softseek.core.logging.AppLog
import io.github.we007oneday_byte.softseek.core.logging.LogEntry
import io.github.we007oneday_byte.softseek.core.logging.LogExportMode
import io.github.we007oneday_byte.softseek.core.logging.LogFilter
import io.github.we007oneday_byte.softseek.core.logging.LogModule
import io.github.we007oneday_byte.softseek.core.logging.LogRepository
import io.github.we007oneday_byte.softseek.core.logging.LogTimeRange
import io.github.we007oneday_byte.softseek.core.logging.LogTypeFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class LogViewerUiState(
    val entries: List<LogEntry> = emptyList(),
    val showInfo: Boolean = false,
    val moduleCode: String? = null,
    val type: LogTypeFilter = LogTypeFilter.ALL,
    val range: LogTimeRange = LogTimeRange.ALL,
    val loading: Boolean = false,
    val exporting: Boolean = false,
    val exportedFile: File? = null,
    val error: String? = null,
) {
    fun visibleEntries(now: Long = System.currentTimeMillis()): List<LogEntry> =
        LogFilter.visible(
            entries = entries,
            showInfo = showInfo,
            moduleCode = moduleCode,
            type = type,
            range = range,
            now = now,
        )
}

@HiltViewModel
class LogViewerViewModel
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : ViewModel() {
        private val repository = LogRepository(context)
        private val _state = MutableStateFlow(LogViewerUiState())
        val state: StateFlow<LogViewerUiState> = _state.asStateFlow()

        init {
            refresh()
        }

        fun refresh() {
            if (_state.value.loading) return
            viewModelScope.launch {
                _state.update { it.copy(loading = true, error = null) }
                runCatching { repository.load() }
                    .onSuccess { entries ->
                        _state.update { it.copy(entries = entries, loading = false) }
                        AppLog.metric(
                            module = LogModule.LOG,
                            name = "log_load_count",
                            value = entries.size,
                            unit = "条",
                        )
                    }
                    .onFailure { error ->
                        _state.update {
                            it.copy(
                                loading = false,
                                error = error.message ?: "日志读取失败",
                            )
                        }
                        AppLog.error(LogModule.LOG, "日志读取失败", error)
                    }
            }
        }

        fun setShowInfo(show: Boolean) {
            _state.update { current ->
                current.copy(
                    showInfo = show,
                    type = if (!show && current.type == LogTypeFilter.INFO) {
                        LogTypeFilter.ALL
                    } else {
                        current.type
                    },
                )
            }
        }

        fun setModule(code: String?) {
            _state.update { it.copy(moduleCode = code) }
        }

        fun setType(type: LogTypeFilter) {
            _state.update {
                it.copy(
                    type = type,
                    showInfo = it.showInfo || type == LogTypeFilter.INFO,
                )
            }
        }

        fun setRange(range: LogTimeRange) {
            _state.update { it.copy(range = range) }
        }

        fun export(mode: LogExportMode) {
            if (_state.value.exporting) return
            viewModelScope.launch {
                _state.update { it.copy(exporting = true, error = null) }
                runCatching { repository.export(mode) }
                    .onSuccess { result ->
                        _state.update { it.copy(exporting = false, exportedFile = result.file) }
                        AppLog.metric(
                            module = LogModule.LOG,
                            name = "log_export_count",
                            value = result.count,
                            unit = "条",
                        )
                    }
                    .onFailure { error ->
                        _state.update {
                            it.copy(
                                exporting = false,
                                error = error.message ?: "日志导出失败",
                            )
                        }
                        AppLog.error(LogModule.LOG, "日志导出失败", error)
                    }
            }
        }

        fun dismissExportedFile() {
            _state.update { it.copy(exportedFile = null) }
        }

        fun clearError() {
            _state.update { it.copy(error = null) }
        }

        fun exportDirectory(): File = repository.exportDirectory()
    }
