package com.proot.cowork.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.proot.cowork.data.prefs.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val savedMessage: String? = null,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val formState = MutableStateFlow(Triple("", "", ""))

    private val savedMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.llmConfig,
        formState,
        savedMessage,
    ) { config, form, message ->
        val (url, key, model) = form
        SettingsUiState(
            baseUrl = url.ifEmpty { config.baseUrl },
            apiKey = key.ifEmpty { config.apiKey },
            model = model.ifEmpty { config.model },
            savedMessage = message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun onBaseUrlChange(value: String) {
        formState.update { (_, k, m) -> Triple(value, k, m) }
    }

    fun onApiKeyChange(value: String) {
        formState.update { (u, _, m) -> Triple(u, value, m) }
    }

    fun onModelChange(value: String) {
        formState.update { (u, k, _) -> Triple(u, k, value) }
    }

    fun save() {
        viewModelScope.launch {
            val state = uiState.value
            settingsRepository.saveLlmConfig(state.baseUrl, state.apiKey, state.model)
            formState.value = Triple("", "", "")
            savedMessage.value = "Settings saved"
        }
    }

    fun clearSavedMessage() {
        savedMessage.value = null
    }

    companion object {
        fun factory(settingsRepository: SettingsRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(settingsRepository) as T
            }
        }
    }
}
