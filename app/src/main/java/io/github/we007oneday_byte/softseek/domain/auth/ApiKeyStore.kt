package io.github.we007oneday_byte.softseek.domain.auth

import kotlinx.coroutines.flow.StateFlow

interface ApiKeyStore {
    val apiKey: StateFlow<String?>

    val keySummaries: StateFlow<List<ApiKeySummary>>

    fun setApiKey(value: String?)

    fun addKey(label: String, key: String, note: String? = null): String

    /**
     * 编辑已有 Key。key 为空表示只改名称/备注，不替换已保存的密钥。
     */
    fun updateKey(id: String, label: String, key: String?, note: String?)

    fun activateKey(id: String)

    fun deleteKey(id: String)
}
