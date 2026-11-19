package io.github.we007oneday_byte.softseek.domain.auth

import kotlinx.coroutines.flow.StateFlow

interface ApiKeyStore {
    val apiKey: StateFlow<String?>

    fun setApiKey(value: String?)
}
