package com.proot.cowork.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "proot_cowork_prefs")

data class LlmConfig(
    val baseUrl: String = "https://openrouter.ai/api/v1",
    val apiKey: String = "",
    val model: String = "openrouter/owl-alpha",
)

data class RootfsState(
    val isInstalled: Boolean = false,
    val isImporting: Boolean = false,
    val importProgress: Float = 0f,
    val distroName: String = "",
)

class SettingsRepository(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        "proot_cowork_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private object Keys {
        val BASE_URL = stringPreferencesKey("base_url")
        val MODEL = stringPreferencesKey("model")
        val API_KEY = "api_key"
        val ROOTFS_INSTALLED = stringPreferencesKey("rootfs_installed")
        val ROOTFS_NAME = stringPreferencesKey("rootfs_name")
    }

    val llmConfig: Flow<LlmConfig> = context.dataStore.data.map { prefs ->
        LlmConfig(
            baseUrl = prefs[Keys.BASE_URL] ?: "https://openrouter.ai/api/v1",
            apiKey = securePrefs.getString(Keys.API_KEY, "") ?: "",
            model = prefs[Keys.MODEL] ?: "openrouter/owl-alpha",
        )
    }

    val rootfsState: Flow<RootfsState> = context.dataStore.data.map { prefs ->
        RootfsState(
            isInstalled = prefs[Keys.ROOTFS_INSTALLED] == "true",
            distroName = prefs[Keys.ROOTFS_NAME] ?: "",
        )
    }

    suspend fun saveLlmConfig(baseUrl: String, apiKey: String, model: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BASE_URL] = baseUrl
            prefs[Keys.MODEL] = model
        }
        securePrefs.edit().putString(Keys.API_KEY, apiKey).apply()
    }

    suspend fun setRootfsInstalled(name: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ROOTFS_INSTALLED] = "true"
            prefs[Keys.ROOTFS_NAME] = name
        }
    }

    fun getRootfsDir() = context.filesDir.resolve("rootfs")

    fun getSkillsDir() = context.filesDir.resolve("skills")

    fun getArtifactsDir() = context.filesDir.resolve("artifacts")
}
