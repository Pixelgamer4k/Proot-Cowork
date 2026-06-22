package com.proot.cowork.data.rootfs

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

object RootfsValidator {
    fun isValid(rootfsDir: File): Boolean {
        if (!rootfsDir.isDirectory) return false
        repairLayout(rootfsDir)
        val startScript = File(rootfsDir, "start-desktop.sh")
        if (!startScript.isFile) return false
        if (!startScript.canExecute()) {
            startScript.setExecutable(true, false)
        }
        if (!startScript.canExecute()) return false
        val bash = File(rootfsDir, "bin/bash")
        return bash.isFile || File(rootfsDir, "usr/bin/bash").isFile
    }

    fun repairLayout(rootfsDir: File) {
        repairXkbSymlink(rootfsDir)
    }

    fun resolveXkbConfigRoot(rootfsDir: File): File? {
        repairXkbSymlink(rootfsDir)
        val candidates = listOf(
            File(rootfsDir, "usr/share/X11/xkb"),
            File(rootfsDir, "usr/share/xkeyboard-config-2"),
            File(rootfsDir, "etc/X11/xkb"),
        )
        return candidates.firstOrNull { dir ->
            dir.isDirectory && !dir.list().isNullOrEmpty()
        }
    }

    private fun repairXkbSymlink(rootfsDir: File) {
        val xkbPath = File(rootfsDir, "usr/share/X11/xkb")
        val xkbData = File(rootfsDir, "usr/share/xkeyboard-config-2")
        if (!xkbData.isDirectory || xkbPath.isDirectory) return
        if (xkbPath.exists() && !xkbPath.delete()) return
        val parent = xkbPath.parentFile ?: return
        if (!parent.exists() && !parent.mkdirs()) return
        try {
            Files.createSymbolicLink(
                xkbPath.toPath(),
                Paths.get("../xkeyboard-config-2"),
            )
        } catch (_: Exception) {
            // X11 can still use XKB_CONFIG_ROOT pointing at xkeyboard-config-2.
        }
    }
}
