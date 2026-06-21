package com.proot.cowork

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.proot.cowork.ui.ProotCoworkApp
import com.proot.cowork.ui.theme.ProotCoworkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ProotCoworkApp
        setContent {
            ProotCoworkTheme {
                ProotCoworkApp(settingsRepository = app.settingsRepository)
            }
        }
    }
}
