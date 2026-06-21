package com.proot.cowork.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Foreground service for proot + X11 desktop lifecycle.
 * Phase 2: embed termux-x11, run proot with --shared-tmp, start guest desktop.
 */
class ProotDesktopService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Phase 2 implementation
        return START_STICKY
    }
}
