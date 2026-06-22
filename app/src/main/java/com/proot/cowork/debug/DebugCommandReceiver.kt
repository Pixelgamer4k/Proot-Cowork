package com.proot.cowork.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.File

/**
 * Debug-only ADB control plane. Example:
 * adb shell am broadcast -a com.proot.cowork.debug.COMMAND \
 *   -n com.proot.cowork.debug/com.proot.cowork.debug.DebugCommandReceiver \
 *   --es cmd DUMP_STATUS
 */
class DebugCommandReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val command = intent?.getStringExtra(EXTRA_CMD) ?: "DUMP_STATUS"
        val extras = buildMap {
            intent?.getStringExtra(EXTRA_PATH)?.let { put("path", it) }
            intent?.getStringExtra(EXTRA_SHELL)?.let { put("command", it) }
            intent?.getStringExtra(EXTRA_LINES)?.let { put("lines", it) }
        }
        val result = DebugBridge.handle(context.applicationContext, command, extras)
        Log.i(TAG, "cmd=$command -> $result")
        val dir = File(context.filesDir, "debug").also { it.mkdirs() }
        File(dir, "last-command-result.txt").writeText(result)
        DebugStatusWriter.refresh(context.applicationContext)
    }

    companion object {
        const val ACTION = "com.proot.cowork.debug.COMMAND"
        const val EXTRA_CMD = "cmd"
        const val EXTRA_PATH = "path"
        const val EXTRA_SHELL = "command"
        const val EXTRA_LINES = "lines"
        private const val TAG = "ProotCoworkDebug"
    }
}
