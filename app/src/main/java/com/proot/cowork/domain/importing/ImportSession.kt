package com.proot.cowork.domain.importing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ImportPhase {
    PREPARING,
    EXTRACTING,
    INSTALLING,
    FINALIZING,
    STARTING_DESKTOP,
}

data class ImportUiState(
    val active: Boolean = false,
    val phase: ImportPhase = ImportPhase.PREPARING,
    val progress: Float = 0f,
    val detail: String = "",
)

object ImportSession {
    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    fun begin() {
        _state.value = ImportUiState(active = true, phase = ImportPhase.PREPARING, progress = 0f)
    }

    fun update(phase: ImportPhase, progress: Float, detail: String = "") {
        _state.value = ImportUiState(
            active = true,
            phase = phase,
            progress = progress.coerceIn(0f, 1f),
            detail = detail,
        )
    }

    fun reset() {
        _state.value = ImportUiState()
    }
}
