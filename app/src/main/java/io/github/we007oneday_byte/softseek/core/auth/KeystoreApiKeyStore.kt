package io.github.we007oneday_byte.softseek.core.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.we007oneday_byte.softseek.domain.auth.ApiKeyStore
import io.github.we007oneday_byte.softseek.domain.auth.ApiKeySummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * P4-08：API Key 使用 Android Keystore 的 AES/GCM 加密后落盘。
 * 支持多个 Key、备注、切换；明文只在内存 StateFlow 中短暂存在。
 */
@Singleton
class KeystoreApiKeyStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : ApiKeyStore {
        private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        private val records = mutableListOf<Record>()
        private val activeKey = MutableStateFlow<String?>(null)
        private val summaries = MutableStateFlow<List<ApiKeySummary>>(emptyList())

        override val apiKey: StateFlow<String?> = activeKey.asStateFlow()

        override val keySummaries: StateFlow<List<ApiKeySummary>> = summaries.asStateFlow()

        init {
            synchronized(this) {
                loadRecords()
                selectActive(preferences.getString(KEY_ACTIVE_ID, null))
            }
        }

        override fun setApiKey(value: String?) {
            synchronized(this) {
                if (value.isNullOrBlank()) {
                    activeId()?.let { deleteLocked(it) }
                    return
                }
                val active = activeId()
                if (active == null) {
                    addKeyLocked("默认 Key", value, null)
                } else {
                    val record = records.firstOrNull { it.id == active }
                    if (record == null) {
                        addKeyLocked("默认 Key", value, null)
                    } else {
                        updateRecordLocked(record.copy(cipher = encrypt(value), hint = hintOf(value)))
                        activeKey.value = value.trim()
                        publishLocked()
                        saveLocked()
                    }
                }
            }
        }

        override fun addKey(label: String, key: String, note: String?): String {
            synchronized(this) {
                return addKeyLocked(label, key, note)
            }
        }

        override fun updateKey(id: String, label: String, key: String?, note: String?) {
            synchronized(this) {
                val record = records.firstOrNull { it.id == id } ?: return
                val replacement = key?.trim()?.takeIf { it.isNotBlank() }
                val updated = record.copy(
                    label = label.trim().ifBlank { record.label },
                    note = note?.trim()?.takeIf { it.isNotBlank() },
                    hint = replacement?.let { hintOf(it) } ?: record.hint,
                    cipher = replacement?.let { encrypt(it) } ?: record.cipher,
                )
                updateRecordLocked(updated)
                if (activeId() == id && replacement != null) {
                    activeKey.value = replacement
                }
                publishLocked()
                saveLocked()
            }
        }

        override fun activateKey(id: String) {
            synchronized(this) {
                selectActive(id)
            }
        }

        override fun deleteKey(id: String) {
            synchronized(this) {
                deleteLocked(id)
            }
        }

        private fun addKeyLocked(label: String, key: String, note: String?): String {
            val cleanKey = key.trim()
            require(cleanKey.isNotBlank()) { "API Key 不能为空" }
            val record = Record(
                id = UUID.randomUUID().toString(),
                label = label.trim().ifBlank { "API Key" },
                note = note?.trim()?.takeIf { it.isNotBlank() },
                hint = hintOf(cleanKey),
                createdAt = System.currentTimeMillis(),
                cipher = encrypt(cleanKey),
            )
            records.add(record)
            preferences.edit { putString(KEY_ACTIVE_ID, record.id) }
            activeKey.value = cleanKey
            publishLocked()
            saveLocked()
            return record.id
        }

        private fun deleteLocked(id: String) {
            records.removeAll { it.id == id }
            saveLocked()
            if (activeId() == id) {
                val next = records.maxByOrNull { it.createdAt }?.id
                if (next == null) {
                    preferences.edit { remove(KEY_ACTIVE_ID) }
                    activeKey.value = null
                } else {
                    selectActive(next)
                }
            } else {
                publishLocked()
                saveLocked()
            }
        }

        private fun selectActive(id: String?) {
            if (records.isEmpty()) {
                preferences.edit { remove(KEY_ACTIVE_ID) }
                activeKey.value = null
                publishLocked()
                return
            }
            val target = records.firstOrNull { it.id == id } ?: records.maxByOrNull { it.createdAt }!!
            val plain = decrypt(target.cipher)
            if (plain == null) {
                records.removeAll { it.id == target.id }
                saveLocked()
                selectActive(null)
                return
            }
            preferences.edit { putString(KEY_ACTIVE_ID, target.id) }
            activeKey.value = plain
            publishLocked()
        }

        private fun activeId(): String? = preferences.getString(KEY_ACTIVE_ID, null)

        private fun publishLocked() {
            val active = activeId()
            summaries.value = records
                .sortedByDescending { it.createdAt }
                .map {
                    ApiKeySummary(
                        id = it.id,
                        label = it.label,
                        note = it.note,
                        hint = it.hint,
                        createdAt = it.createdAt,
                        isActive = it.id == active,
                    )
                }
        }

        private fun loadRecords() {
            records.clear()
            val raw = preferences.getString(KEY_RECORDS, null) ?: return
            runCatching {
                val array = JSONArray(raw)
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    records.add(
                        Record(
                            id = item.getString("id"),
                            label = item.optString("label", "API Key"),
                            note = item.optString("note").takeIf { it.isNotBlank() },
                            hint = item.optString("hint"),
                            createdAt = item.optLong("createdAt"),
                            cipher = item.getString("cipher"),
                        ),
                    )
                }
            }.onFailure {
                records.clear()
            }
        }

        private fun saveLocked() {
            val array = JSONArray()
            records.forEach {
                array.put(
                    JSONObject().apply {
                        put("id", it.id)
                        put("label", it.label)
                        put("note", it.note.orEmpty())
                        put("hint", it.hint)
                        put("createdAt", it.createdAt)
                        put("cipher", it.cipher)
                    },
                )
            }
            preferences.edit { putString(KEY_RECORDS, array.toString()) }
        }

        private fun updateRecordLocked(record: Record) {
            val index = records.indexOfFirst { it.id == record.id }
            if (index >= 0) records[index] = record
        }

        private fun encrypt(value: String): String {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, masterKey())
            val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
        }

        private fun decrypt(value: String): String? = runCatching {
            val payload = Base64.decode(value, Base64.NO_WRAP)
            require(payload.size > IV_SIZE)
            val iv = payload.copyOfRange(0, IV_SIZE)
            val encrypted = payload.copyOfRange(IV_SIZE, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, masterKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.getOrNull()

        private fun masterKey(): SecretKey {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { return it }
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            generator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            return generator.generateKey()
        }

        private fun hintOf(value: String): String = "••••" + value.takeLast(4)

        private data class Record(
            val id: String,
            val label: String,
            val note: String?,
            val hint: String,
            val createdAt: Long,
            val cipher: String,
        )

        private companion object {
            const val PREFS_NAME = "softseek_api_keys"
            const val KEY_RECORDS = "records"
            const val KEY_ACTIVE_ID = "active_id"
            const val KEY_ALIAS = "softseek_api_key_master"
            const val ANDROID_KEYSTORE = "AndroidKeyStore"
            const val TRANSFORMATION = "AES/GCM/NoPadding"
            const val IV_SIZE = 12
        }
    }
