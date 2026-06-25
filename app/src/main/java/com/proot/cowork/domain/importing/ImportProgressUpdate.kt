package com.proot.cowork.domain.importing

data class ImportProgressUpdate(
    val phase: ImportPhase,
    val progress: Float,
    val detail: String = "",
)
