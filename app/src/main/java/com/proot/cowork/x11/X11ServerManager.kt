package com.proot.cowork.x11

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import com.termux.x11.CmdEntryPoint
import com.termux.x11.ICmdEntryInterface
import com.termux.x11.LorieView

object X11ServerManager {

    private const val TAG = "X11ServerManager"
    private var serverThread: Thread? = null
    private var service: ICmdEntryInterface? = null
    private var receiverRegistered = false
    private var receiver: BroadcastReceiver? = null

    @Synchronized
    fun ensureStarted(context: Context) {
        if (serverThread?.isAlive == true) return

        val appContext = context.applicationContext
        registerReceiver(appContext)

        serverThread = Thread({
            try {
                System.setProperty("TERMUX_X11_OVERRIDE_PACKAGE", appContext.packageName)
                CmdEntryPoint.main(arrayOf(":0"))
            } catch (e: Exception) {
                Log.e(TAG, "X11 server thread failed", e)
            }
        }, "x11-server").also { it.start() }

        // Give the server a moment to publish its binder.
        Thread.sleep(500)
    }

    @Synchronized
    fun stop() {
        service = null
        serverThread?.interrupt()
        serverThread = null
    }

    fun connectLorieView(lorieView: LorieView): Boolean {
        if (LorieView.connected()) return true
        val svc = service ?: return false
        return try {
            val fd: ParcelFileDescriptor? = svc.xConnection
            if (fd != null) {
                LorieView.connect(fd.detachFd())
                lorieView.triggerCallback()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect LorieView", e)
            false
        }
    }

    private fun registerReceiver(context: Context) {
        if (receiverRegistered) return
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != CmdEntryPoint.ACTION_START) return
                val bundle = intent.getBundleExtra(null) ?: return
                val binder: IBinder = bundle.getBinder(null) ?: return
                service = ICmdEntryInterface.Stub.asInterface(binder)
                Log.i(TAG, "X11 CmdEntryPoint connected")
            }
        }
        val filter = IntentFilter(CmdEntryPoint.ACTION_START)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        receiverRegistered = true
    }
}
