package io.github.we007oneday_byte.softseek.core.constant

object AppConstants {
    const val APP_NAME = "SOFTSEEK"
    const val APP_VERSION = "0.1.0"
    const val DEFAULT_BASE_URL = "https://api.deepseek.com"

    /** OpenAI Chat Completions 兼容服务的默认示例模型；可在设置中改为任意模型 ID。 */
    const val DEFAULT_MODEL = "deepseek-chat"

    const val OFFICIAL_DISCLAIMER =
        "本应用为第三方 AI API 兼容客户端，支持 OpenAI Chat Completions 与 Anthropic Messages 协议，与任何模型服务商无隶属关系。所有 AI 服务由你配置的服务商提供。"
}
