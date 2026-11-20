package io.github.we007oneday_byte.softseek.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.we007oneday_byte.softseek.core.settings.AppSettings
import io.github.we007oneday_byte.softseek.core.settings.VisualStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.softSeekSettings: DataStore<Preferences> by preferencesDataStore(
    name = "softseek_settings",
)

@Singleton
class SettingsRepository
    @Inject
    constructor(
        @ApplicationContext context: Context,
        private val json: Json,
    ) {
        private val dataStore = context.applicationContext.softSeekSettings
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val key = stringPreferencesKey(SETTINGS_KEY)

        val settings: StateFlow<AppSettings> = dataStore.data
            .catch { error ->
                if (error is IOException) emit(emptyPreferences()) else throw error
            }
            .map { preferences -> decode(preferences[key]) }
            .stateIn(scope, SharingStarted.Eagerly, AppSettings())

        suspend fun update(transform: (AppSettings) -> AppSettings) {
            val next = transform(settings.value)
            dataStore.edit { preferences ->
                preferences[key] = json.encodeToString(next)
            }
        }

        private fun decode(raw: String?): AppSettings {
            if (raw.isNullOrBlank()) return AppSettings()
            val decoded = runCatching { json.decodeFromString<AppSettings>(raw) }.getOrNull()
            if (decoded != null) return decoded
            // 主题枚举被删除/未来新增时不让整份设置回退，至少保留可识别主题。
            return AppSettings(visualStyle = decodeVisualStyle(raw))
        }

        private fun decodeVisualStyle(raw: String): VisualStyle = runCatching {
            val name = json.parseToJsonElement(raw)
                .jsonObject["visualStyle"]
                ?.jsonPrimitive
                ?.contentOrNull
            VisualStyle.fromName(name)
        }.getOrDefault(VisualStyle.Minimal)

        private companion object {
            const val SETTINGS_KEY = "settings_json"
        }
    }
