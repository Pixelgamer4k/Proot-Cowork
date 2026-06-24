package com.proot.cowork.termux.bootstrap

import android.content.Context
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.util.zip.GZIPInputStream

object AssetExtractor {

    fun extractGzipTar(input: InputStream, targetDir: File): Boolean {
        val marker = File(targetDir, ".extraction_complete")
        if (marker.exists()) return true

        targetDir.mkdirs()
        TarArchiveInputStream(GZIPInputStream(BufferedInputStream(input))).use { tar ->
            var entry = tar.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else if (entry.isSymbolicLink) {
                    outFile.parentFile?.mkdirs()
                    outFile.delete()
                    Runtime.getRuntime().exec(
                        arrayOf("ln", "-sf", entry.linkName, outFile.absolutePath),
                    ).waitFor()
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
        marker.createNewFile()
        return true
    }
}

object TermuxBootstrap {

    fun prefixDir(context: Context): File = File(context.filesDir, "usr")

    fun bashExecutable(context: Context): File = File(prefixDir(context), "bin/bash")

    fun isInstalled(context: Context): Boolean {
        val marker = File(prefixDir(context), ".extraction_complete")
        return marker.isFile && bashExecutable(context).canExecute()
    }

    fun ensureInstalled(context: Context): Boolean {
        val prefix = prefixDir(context)
        val marker = File(prefix, ".extraction_complete")
        if (marker.isFile) {
            linkBash(context)
            ensureLayout(prefix)
            return bashExecutable(context).canExecute()
        }

        context.assets.open("bootstrap.bin").use { input ->
            if (!AssetExtractor.extractGzipTar(input, prefix)) return false
        }
        linkBash(context)
        ensureLayout(prefix)
        return bashExecutable(context).canExecute()
    }

    private fun linkBash(context: Context) {
        val nativeBash = File(context.applicationInfo.nativeLibraryDir, "libbash.so")
        val binDir = File(prefixDir(context), "bin").also { it.mkdirs() }
        val bash = File(binDir, "bash")
        if (nativeBash.isFile) {
            if (bash.exists()) bash.delete()
            Runtime.getRuntime().exec(
                arrayOf("ln", "-sf", nativeBash.absolutePath, bash.absolutePath),
            ).waitFor()
        }
    }

    private fun ensureLayout(prefix: File) {
        File(prefix, "tmp/.X11-unix").mkdirs()
        File(prefix, "home").mkdirs()
    }

    fun shellEnvironment(context: Context): Array<String> {
        val prefix = prefixDir(context).absolutePath
        val home = File(prefix, "home").absolutePath
        val tmp = File(prefix, "tmp").absolutePath
        return arrayOf(
            "HOME=$home",
            "PREFIX=$prefix",
            "PATH=$prefix/bin",
            "TMPDIR=$tmp",
            "DISPLAY=:0",
            "TERM=xterm-256color",
            "LANG=en_US.UTF-8",
        )
    }
}
