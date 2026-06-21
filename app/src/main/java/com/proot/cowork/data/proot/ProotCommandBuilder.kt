package com.proot.cowork.data.proot

import android.content.Context
import android.os.Environment
import java.io.File

object ProotCommandBuilder {

    fun buildStartDesktop(
        context: Context,
        runtime: ProotRuntime,
        rootfsDir: File,
    ): List<String> {
        val tmp = runtime.tmpDir
        val x11Unix = File(tmp, ".X11-unix").also { it.mkdirs() }

        val bindings = listOf(
            "/dev:/dev",
            "/proc:/proc",
            "/sys:/sys",
            "${tmp.absolutePath}:/tmp",
            "${context.filesDir.absolutePath}/artifacts:/artifacts",
            "${context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath}/storage:/storage",
        )

        val env = mapOf(
            "DISPLAY" to ":0",
            "XDG_RUNTIME_DIR" to "/tmp",
            "LD_LIBRARY_PATH" to runtime.libraryPath.absolutePath,
            "HOME" to "/home/cowork",
            "USER" to "cowork",
            "TMPDIR" to "/tmp",
        )

        val argv = mutableListOf(runtime.prootBinary.absolutePath)
        argv += "-r"
        argv += rootfsDir.absolutePath
        bindings.forEach { bind ->
            argv += "-b"
            argv += bind
        }
        argv += "-w"
        argv += "/"
        env.forEach { (k, v) ->
            argv += "-0"
            argv += "$k=$v"
        }
        argv += "/start-desktop.sh"
        return argv
    }

    fun buildShell(
        runtime: ProotRuntime,
        rootfsDir: File,
        command: String,
    ): List<String> {
        val tmp = runtime.tmpDir
        val bindings = listOf(
            "/dev:/dev",
            "/proc:/proc",
            "/sys:/sys",
            "${tmp.absolutePath}:/tmp",
        )
        val argv = mutableListOf(runtime.prootBinary.absolutePath)
        argv += "-r"
        argv += rootfsDir.absolutePath
        bindings.forEach { bind ->
            argv += "-b"
            argv += bind
        }
        argv += "-w"
        argv += "/root"
        argv += "-0"
        argv += "LD_LIBRARY_PATH=${runtime.libraryPath.absolutePath}"
        argv += "-0"
        argv += "HOME=/home/cowork"
        argv += "/bin/bash"
        argv += "-lc"
        argv += command
        return argv
    }
}
