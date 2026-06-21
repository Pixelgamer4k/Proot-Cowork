package com.proot.cowork.data.proot

import android.content.Context
import android.os.Build
import java.io.File

class RuntimeBootstrap(private val context: Context) {

    private val runtimeRoot: File
        get() = File(context.filesDir, "runtime")

    fun ensureRuntime(): ProotRuntime {
        val abi = when (Build.SUPPORTED_ABIS.firstOrNull()) {
            "arm64-v8a" -> "aarch64"
            "armeabi-v7a" -> "arm"
            "x86_64" -> "x86_64"
            "x86" -> "i686"
            else -> "aarch64"
        }
        val assetPrefix = "runtime/$abi"
        val outDir = File(runtimeRoot, abi)
        val binDir = File(outDir, "bin")
        val libDir = File(outDir, "lib")
        val prootBin = File(binDir, "proot")

        if (!prootBin.exists()) {
            outDir.mkdirs()
            binDir.mkdirs()
            libDir.mkdirs()
            copyAssetTree(context, assetPrefix, outDir)
            prootBin.setExecutable(true, false)
            libDir.listFiles()?.forEach { it.setReadable(true, false) }
        }

        return ProotRuntime(
            prootBinary = prootBin,
            libraryPath = libDir,
            tmpDir = File(context.filesDir, "tmp").also { it.mkdirs() },
        )
    }

    private fun copyAssetTree(context: Context, assetPath: String, dest: File) {
        val assets = context.assets.list(assetPath) ?: return
        for (name in assets) {
            val childAsset = "$assetPath/$name"
            val childDest = File(dest, name)
            val nested = context.assets.list(childAsset)
            if (nested.isNullOrEmpty()) {
                context.assets.open(childAsset).use { input ->
                    childDest.outputStream().use { output -> input.copyTo(output) }
                }
            } else {
                childDest.mkdirs()
                copyAssetTree(context, childAsset, childDest)
            }
        }
    }
}

data class ProotRuntime(
    val prootBinary: File,
    val libraryPath: File,
    val tmpDir: File,
)
