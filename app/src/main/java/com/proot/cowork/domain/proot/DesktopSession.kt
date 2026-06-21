package com.proot.cowork.domain.proot

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DesktopSession {
    private val _state = MutableStateFlow(DesktopState.NO_ROOTFS)
    val state: StateFlow<DesktopState> = _state.asStateFlow()

    private val _logLines = MutableStateFlow<List<String>>(emptyList())
    val logLines: StateFlow<List<String>> = _logLines.asStateFlow()

    fun setState(state: DesktopState) {
        _state.value = state
    }

    fun appendLog(line: String) {
        _logLines.value = (_logLines.value + line).takeLast(200)
    }

    fun clearLogs() {
        _logLines.value = emptyList()
    }
}
