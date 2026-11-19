package io.github.we007oneday_byte.softseek.domain.auth

import kotlinx.coroutines.flow.StateFlow

interface ApiKeyStore {
    val apiKey: StateFlow<String?>

    val keySummaries: StateFlow<List<ApiKeySummary>>

    fun setApiKey(value: String?)

    fun addKey(label: String, key: String, note: String? = null): String

    fun activateKey(id: String)

    fun deleteKey(id: String)
}
