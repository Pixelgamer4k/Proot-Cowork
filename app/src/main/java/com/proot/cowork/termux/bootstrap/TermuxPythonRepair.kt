package com.proot.cowork.termux.bootstrap

import android.content.Context
import android.util.Log
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.BufferedInputStream
import java.io.File
import java.util.zip.GZIPInputStream

/**
 * Restores a clean Python runtime when [TermuxElfPathPatch] v3 corrupted libpython frozen modules.
 * CI bundles an unpatched copy in assets/python-runtime.tar.gz.
 */
object TermuxPythonRepair {

    private const val TAG = "TermuxPythonRepair"
    private const val ASSET = "python-runtime.tar.gz"

    fun applyIfNeeded(context: Context, prefix: File, elfRoot: String, filesRoot: String): Boolean {
        val marker = File(prefix, ".termux_python_repaired_v1")
        if (marker.isFile && probePython(context, prefix)) return true

        if (!restoreFromAsset(context, prefix)) {
            Log.w(TAG, "python-runtime asset missing; applying safe path patch only")
            TermuxElfPathPatch.patchPythonRuntime(prefix, elfRoot, filesRoot)
            marker.createNewFile()
            return probePython(context, prefix)
        }

        TermuxElfPathPatch.patchPythonRuntime(prefix, elfRoot, filesRoot)
        marker.createNewFile()
        val ok = probePython(context, prefix)
        if (!ok) {
            Log.e(TAG, "python still broken after repair")
        } else {
            Log.i(TAG, "python runtime repaired")
        }
        return ok
    }

    private fun restoreFromAsset(context: Context, prefix: File): Boolean {
        return try {
            context.assets.open(ASSET).use { input ->
                TarArchiveInputStream(GZIPInputStream(BufferedInputStream(input))).use { tar ->
                    var entry = tar.nextEntry
                    while (entry != null) {
                        val outFile = File(prefix, entry.name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            outFile.outputStream().use { out -> tar.copyTo(out) }
                            if (entry.mode and 0b001_001_001 != 0) {
                                outFile.setExecutable(true, false)
                            }
                        }
                        entry = tar.nextEntry
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "failed to extract $ASSET: ${e.message}")
            false
        }
    }

    private fun probePython(context: Context, prefix: File): Boolean {
        val python = File(prefix, "bin/python3.13")
        if (!python.canExecute()) return false
        val bash = TermuxBootstrap.shellExecutable(context) ?: return false
        val env = TermuxShellEnvironment.buildProcessEnvironment(context)
        val pb = ProcessBuilder(
            bash.absolutePath,
            "-c",
            "${python.absolutePath} -c pass",
        )
        pb.directory(TermuxBootstrap.homeDir(context))
        pb.environment().clear()
        pb.environment().putAll(env)
        return try {
            pb.start().waitFor() == 0
        } catch (e: Exception) {
            Log.w(TAG, "python probe failed", e)
            false
        }
    }
}
