package io.github.we007oneday_byte.softseek.domain.provider

import kotlinx.serialization.Serializable

/**
 * 接口协议类型。不同协议使用不同的请求路径、鉴权头和流式事件格式，
 * 但都映射到同一套 [io.github.we007oneday_byte.softseek.domain.model.ChatStreamEvent]。
 */
@Serializable
enum class ProviderType(
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModelId: String,
) {
    OPENAI_COMPATIBLE(
        displayName = "OpenAI 兼容",
        defaultBaseUrl = "https://api.deepseek.com",
        defaultModelId = "deepseek-chat",
    ),
    ANTHROPIC(
        displayName = "Anthropic Messages",
        defaultBaseUrl = "https://api.anthropic.com",
        defaultModelId = "claude-sonnet-4-5",
    ),
}
