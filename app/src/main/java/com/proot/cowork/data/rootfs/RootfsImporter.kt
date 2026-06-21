package com.proot.cowork.data.rootfs

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream

class RootfsImporter(private val context: Context) {

    suspend fun import(
        sourceUri: Uri,
        destDir: File,
        onProgress: suspend (Float) -> Unit,
    ): ImportResult = withContext(Dispatchers.IO) {
        if (destDir.exists()) {
            destDir.deleteRecursively()
        }
        destDir.mkdirs()

        val resolver = context.contentResolver
        val size = resolver.openFileDescriptor(sourceUri, "r")?.use { it.statSize } ?: -1L

        resolver.openInputStream(sourceUri)?.use { raw ->
            BufferedInputStream(raw).use { buffered ->
                GzipCompressorInputStream(buffered).use { gzip ->
                    TarArchiveInputStream(gzip).use { tar ->
                        var entry: TarArchiveEntry? = tar.nextEntry
                        var bytesRead = 0L
                        while (entry != null) {
                            val name = entry.name.removePrefix("./").removePrefix("/")
                            if (name.isEmpty() || name == "." ) {
                                entry = tar.nextEntry
                                continue
                            }
                            val outFile = File(destDir, name)
                            if (entry.isDirectory) {
                                outFile.mkdirs()
                            } else {
                                outFile.parentFile?.mkdirs()
                                FileOutputStream(outFile).use { fos ->
                                    val buffer = ByteArray(64 * 1024)
                                    var read = tar.read(buffer)
                                    while (read != -1) {
                                        fos.write(buffer, 0, read)
                                        bytesRead += read
                                        if (size > 0) {
                                            onProgress((bytesRead.toFloat() / size).coerceIn(0f, 0.99f))
                                        }
                                        read = tar.read(buffer)
                                    }
                                }
                                val mode = entry.mode
                                if (mode and 64 != 0) outFile.setExecutable(true, false)
                                if (mode and 128 != 0) outFile.setReadable(true, false)
                                if (mode and 1 != 0) outFile.setWritable(true, false)
                            }
                            entry = tar.nextEntry
                        }
                    }
                }
            }
        } ?: return@withContext ImportResult.Error("Could not open rootfs file")

        val startScript = File(destDir, "start-desktop.sh")
        if (!startScript.exists()) {
            return@withContext ImportResult.Error("Invalid rootfs: start-desktop.sh not found at root")
        }

        onProgress(1f)
        File(destDir, "start-desktop.sh").setExecutable(true, false)
        ImportResult.Success(destDir)
    }
}

sealed class ImportResult {
    data class Success(val rootfsDir: File) : ImportResult()
    data class Error(val message: String) : ImportResult()
}
