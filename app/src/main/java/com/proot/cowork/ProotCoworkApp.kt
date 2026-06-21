package com.proot.cowork

import android.app.Application
import com.proot.cowork.data.prefs.SettingsRepository

class ProotCoworkApp : Application() {
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
    }
}
