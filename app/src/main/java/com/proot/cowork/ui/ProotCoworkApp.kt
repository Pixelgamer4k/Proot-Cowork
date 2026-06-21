package com.proot.cowork.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.proot.cowork.data.prefs.SettingsRepository
import com.proot.cowork.data.rootfs.RootfsRepository
import com.proot.cowork.ui.home.HomeScreen
import com.proot.cowork.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
}

@Composable
fun ProotCoworkApp(
    settingsRepository: SettingsRepository,
    rootfsRepository: RootfsRepository,
    onImportRootfs: () -> Unit,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                settingsRepository = settingsRepository,
                rootfsRepository = rootfsRepository,
                onImportRootfs = onImportRootfs,
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                settingsRepository = settingsRepository,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
