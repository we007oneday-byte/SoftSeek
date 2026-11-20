package io.github.we007oneday_byte.softseek.ui.screens.settings

import io.github.we007oneday_byte.softseek.R

/**
 * 设置一级入口对应的二级页。
 *
 * 顺序与 ZL-01-09 的分类保持一致；新增分类时 route 和字符串一起补齐，
 * NavGraph 会按 entries 自动注册，不需要再手写一个 route 分支。
 */
enum class SettingsSection(
    val route: String,
    val titleRes: Int,
    val subtitleRes: Int,
) {
    API("settings/api", R.string.settings_api, R.string.settings_api_desc),
    MODEL("settings/model", R.string.settings_model, R.string.settings_model_desc),
    CHAT("settings/chat", R.string.settings_chat, R.string.settings_chat_desc),
    CALL("settings/call", R.string.settings_call, R.string.settings_call_desc),
    MEMORY("settings/memory", R.string.settings_memory, R.string.settings_memory_desc),
    APPEARANCE("settings/appearance", R.string.settings_appearance, R.string.settings_appearance_desc),
    DATA("settings/data", R.string.settings_data, R.string.settings_data_desc),
    LOGS("settings/logs", R.string.settings_logs, R.string.settings_logs_desc),
    ABOUT("settings/about", R.string.settings_about, R.string.settings_about_desc),
}
