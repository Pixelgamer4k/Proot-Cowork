package com.proot.cowork.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.proot.cowork.data.prefs.LlmConfig
import com.proot.cowork.data.prefs.SettingsRepository
import com.proot.cowork.domain.agent.CoworkKoogAgentRunner
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
    val isTestingConnection: Boolean = false,
    val connectionTestMessage: String? = null,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val formState = MutableStateFlow(Triple("", "", ""))
    private val savedMessage = MutableStateFlow<String?>(null)
    private val connectionTestMessage = MutableStateFlow<String?>(null)
    private val isTestingConnection = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.llmConfig,
        formState,
        savedMessage,
        connectionTestMessage,
        isTestingConnection,
    ) { config, form, message, testMessage, testing ->
        val (url, key, model) = form
        SettingsUiState(
            baseUrl = url.ifEmpty { config.baseUrl },
            apiKey = key.ifEmpty { config.apiKey },
            model = model.ifEmpty { config.model },
            savedMessage = message,
            connectionTestMessage = testMessage,
            isTestingConnection = testing,
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

    fun testConnection() {
        viewModelScope.launch {
            val state = uiState.value
            isTestingConnection.value = true
            connectionTestMessage.value = null
            val result = CoworkKoogAgentRunner.testConnection(
                LlmConfig(
                    baseUrl = state.baseUrl,
                    apiKey = state.apiKey,
                    model = state.model,
                ),
            )
            connectionTestMessage.value = result.fold(
                onSuccess = { it },
                onFailure = { "Connection failed: ${it.message}" },
            )
            isTestingConnection.value = false
        }
    }

    fun clearSavedMessage() {
        savedMessage.value = null
    }

    fun clearConnectionTestMessage() {
        connectionTestMessage.value = null
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
