package io.github.we007oneday_byte.softseek.core.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object BaseUrlValidator {
    fun validate(value: String): String {
        val url = value.trim().toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Base URL 格式不正确")
        require(url.isHttps) { "Base URL 必须使用 HTTPS" }
        require(url.host.isNotBlank()) { "Base URL 缺少主机名" }
        val normalized = url.toString()
        return if (normalized.endsWith("/")) normalized else "$normalized/"
    }
}
