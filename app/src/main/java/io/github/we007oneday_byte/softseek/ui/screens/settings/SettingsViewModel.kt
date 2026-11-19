package io.github.we007oneday_byte.softseek.ui.screens.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.we007oneday_byte.softseek.domain.auth.ApiKeyStore
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val apiKeyStore: ApiKeyStore,
    ) : ViewModel() {
        val apiKey: StateFlow<String?> = apiKeyStore.apiKey

        fun saveApiKey(value: String) {
            apiKeyStore.setApiKey(value)
        }

        fun clearApiKey() {
            apiKeyStore.setApiKey(null)
        }
    }
