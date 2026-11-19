package io.github.we007oneday_byte.softseek.domain.model

sealed class ChatError {
    data object NoApiKey : ChatError()

    data object InvalidApiKey : ChatError()

    data object InsufficientBalance : ChatError()

    data object RateLimited : ChatError()

    data class BadRequest(val message: String) : ChatError()

    data class Server(val statusCode: Int, val message: String) : ChatError()

    data class Network(val message: String) : ChatError()

    data class Protocol(val message: String) : ChatError()

    data object Cancelled : ChatError()
}

fun ChatError.userMessage(): String = when (this) {
    ChatError.NoApiKey -> "尚未配置 API Key"
    ChatError.InvalidApiKey -> "API Key 无效或已失效"
    ChatError.InsufficientBalance -> "账户余额不足"
    ChatError.RateLimited -> "请求过于频繁，请稍后重试"
    is ChatError.BadRequest -> message.ifBlank { "请求参数有误" }
    is ChatError.Server -> "服务暂时不可用（HTTP $statusCode）"
    is ChatError.Network -> "网络连接失败：$message"
    is ChatError.Protocol -> "响应解析失败：$message"
    ChatError.Cancelled -> "已停止生成"
}
