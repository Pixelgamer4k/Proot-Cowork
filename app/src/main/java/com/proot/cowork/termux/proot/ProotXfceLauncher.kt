package com.proot.cowork.termux.proot

import android.content.Context
import android.util.Log
import com.proot.cowork.domain.desktop.StackFrontLayer
import com.proot.cowork.domain.desktop.TermuxStackSession
import com.proot.cowork.domain.proot.DesktopSession
import com.proot.cowork.termux.bootstrap.CoworkAssetInstaller
import com.proot.cowork.termux.bootstrap.TermuxBootstrap
import com.proot.cowork.termux.bootstrap.TermuxShellEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/** Starts XFCE inside proot-distro on the embedded X11 display (no visible terminal). */
object ProotXfceLauncher {

    private const val TAG = "ProotXfceLauncher"
    private var desktopProcess: Process? = null
    private var logThread: Thread? = null

    fun isRunning(): Boolean = desktopProcess?.isAlive == true

    suspend fun start(
        context: Context,
        distro: String = "ubuntu",
        waitForX11Ms: Long = 90_000L,
    ): Boolean = withContext(Dispatchers.IO) {
        if (isRunning()) return@withContext true

        if (!waitForX11Socket(context, waitForX11Ms)) {
            DesktopSession.appendLog("X11 not ready — cannot start desktop")
            return@withContext false
        }

        val prefix = TermuxBootstrap.prefixDir(context)
        CoworkAssetInstaller.installIfNeeded(context, prefix)

        val script = File(prefix, "share/cowork/proot-xfce-start.sh")
        if (!script.isFile) {
            DesktopSession.appendLog("proot-xfce-start.sh missing")
            return@withContext false
        }

        val bash = TermuxBootstrap.shellExecutable(context)
        if (bash == null || !bash.canExecute()) {
            DesktopSession.appendLog("Termux bash not ready")
            return@withContext false
        }

        stop()

        val env = TermuxShellEnvironment.buildProcessEnvironment(context)
        val pb = ProcessBuilder(bash.absolutePath, script.absolutePath, distro)
        pb.directory(TermuxBootstrap.homeDir(context))
        pb.environment().clear()
        pb.environment().putAll(env)
        pb.redirectErrorStream(true)

        return@withContext try {
            val process = pb.start()
            desktopProcess = process
            logThread = Thread {
                runCatching {
                    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                        reader.lineSequence().forEach { line ->
                            if (line.isNotBlank()) {
                                DesktopSession.appendLog(line)
                                TermuxStackSession.appendLog(line)
                            }
                        }
                    }
                }
            }.apply {
                isDaemon = true
                start()
            }
            TermuxStackSession.setFrontLayer(StackFrontLayer.X11)
            DesktopSession.appendLog("Starting Ubuntu XFCE on :0…")
            delay(2_500)
            process.isAlive
        } catch (e: Exception) {
            Log.e(TAG, "start failed", e)
            DesktopSession.appendLog("Desktop start failed: ${e.message}")
            false
        }
    }

    fun stop() {
        runCatching { desktopProcess?.destroy() }
        desktopProcess = null
        logThread = null
    }

    suspend fun restart(context: Context, distro: String = "ubuntu"): Boolean {
        stop()
        delay(800)
        return start(context, distro)
    }

    private suspend fun waitForX11Socket(context: Context, timeoutMs: Long): Boolean {
        val socket = File(TermuxBootstrap.prefixDir(context), "tmp/.X11-unix/X0")
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (socket.exists()) return true
            delay(250)
        }
        return socket.exists()
    }
}
