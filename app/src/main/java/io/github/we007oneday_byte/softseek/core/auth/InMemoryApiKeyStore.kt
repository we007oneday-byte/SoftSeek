package io.github.we007oneday_byte.softseek.core.auth

import io.github.we007oneday_byte.softseek.domain.auth.ApiKeyStore
import io.github.we007oneday_byte.softseek.domain.auth.ApiKeySummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 测试与降级用内存实现。正式绑定见 [KeystoreApiKeyStore]。
 */
@Singleton
class InMemoryApiKeyStore
    @Inject
    constructor() : ApiKeyStore {
        private val state = MutableStateFlow<String?>(null)
        private val summaries = MutableStateFlow<List<ApiKeySummary>>(emptyList())
        private var activeId: String? = null
        private val notes = mutableMapOf<String, Pair<String, String?>>()

        override val apiKey: StateFlow<String?> = state.asStateFlow()

        override val keySummaries: StateFlow<List<ApiKeySummary>> = summaries.asStateFlow()

        override fun setApiKey(value: String?) {
            if (value.isNullOrBlank()) {
                activeId?.let { deleteKey(it) }
                return
            }
            val id = activeId ?: addKey("默认 Key", value)
            notes[id] = (notes[id]?.first ?: "默认 Key") to notes[id]?.second
            state.value = value.trim()
            publish()
        }

        override fun addKey(label: String, key: String, note: String?): String {
            val id = UUID.randomUUID().toString()
            notes[id] = label to note
            activeId = id
            state.value = key.trim()
            publish()
            return id
        }

        override fun updateKey(id: String, label: String, key: String?, note: String?) {
            val existing = notes[id] ?: return
            notes[id] = (label.ifBlank { existing.first }) to note
            if (activeId == id && !key.isNullOrBlank()) {
                state.value = key.trim()
            }
            publish()
        }

        override fun activateKey(id: String) {
            if (!notes.containsKey(id)) return
            activeId = id
            publish()
        }

        override fun deleteKey(id: String) {
            notes.remove(id)
            if (activeId == id) {
                activeId = notes.keys.firstOrNull()
                state.value = null
            }
            publish()
        }

        private fun publish() {
            summaries.value = notes.map { (id, value) ->
                ApiKeySummary(
                    id = id,
                    label = value.first,
                    note = value.second,
                    hint = "••••内存",
                    createdAt = 0,
                    isActive = id == activeId,
                )
            }
        }
    }
