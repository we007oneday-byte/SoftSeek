package io.github.we007oneday_byte.softseek.core.auth

import io.github.we007oneday_byte.softseek.domain.auth.ApiKeyStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * P3 阶段的最小实现：密钥只存在进程内存，退出应用即失效。
 * P4-08 将替换为 Android Keystore 加密持久化实现。
 */
@Singleton
class InMemoryApiKeyStore
    @Inject
    constructor() : ApiKeyStore {
        private val state = MutableStateFlow<String?>(null)

        override val apiKey: StateFlow<String?> = state.asStateFlow()

        override fun setApiKey(value: String?) {
            state.value = value?.trim()?.takeIf { it.isNotBlank() }
        }
    }
