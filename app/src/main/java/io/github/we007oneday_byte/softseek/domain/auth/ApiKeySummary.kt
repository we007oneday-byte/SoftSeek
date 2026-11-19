package io.github.we007oneday_byte.softseek.domain.auth

data class ApiKeySummary(
    val id: String,
    val label: String,
    val note: String?,
    val hint: String,
    val createdAt: Long,
    val isActive: Boolean,
)
