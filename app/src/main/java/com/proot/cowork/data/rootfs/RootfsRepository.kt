package com.proot.cowork.data.rootfs

import android.content.Context
import android.net.Uri
import com.proot.cowork.data.prefs.SettingsRepository
import com.proot.cowork.domain.proot.DesktopSession
import com.proot.cowork.domain.proot.DesktopState
import com.proot.cowork.service.ProotDesktopService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RootfsRepository(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) {
    private val importer = RootfsImporter(context)

    suspend fun importFromUri(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        settingsRepository.setImporting(true, 0f)
        DesktopSession.setState(DesktopState.IMPORTING)

        val result = importer.import(
            sourceUri = uri,
            destDir = settingsRepository.getRootfsDir(),
        ) { progress ->
            settingsRepository.setImporting(true, progress)
        }

        when (result) {
            is ImportResult.Success -> {
                settingsRepository.setRootfsInstalled("ubuntu")
                DesktopSession.setState(DesktopState.STARTING)
                startDesktopService()
            }
            is ImportResult.Error -> {
                settingsRepository.setImporting(false, 0f)
                DesktopSession.setState(DesktopState.NO_ROOTFS)
            }
        }
        result
    }

    fun startDesktopService() {
        val intent = android.content.Intent(context, ProotDesktopService::class.java)
        context.startForegroundService(intent)
    }

    fun stopDesktopService() {
        val intent = android.content.Intent(context, ProotDesktopService::class.java).apply {
            action = ProotDesktopService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun rebootDesktopService() {
        val intent = android.content.Intent(context, ProotDesktopService::class.java).apply {
            action = ProotDesktopService.ACTION_REBOOT
        }
        context.startForegroundService(intent)
    }
}
