package com.englishai.translator

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.englishai.translator.web.MoonPointerWebAssets

class WebAppViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var apiServerUrl by mutableStateOf(
        prefs.getString(KEY_API, DEFAULT_API) ?: DEFAULT_API
    )
        private set

    var webAppUrl by mutableStateOf(loadWebAppUrl())
        private set

    private fun loadWebAppUrl(): String {
        val migrated = MoonPointerWebAssets.migrateStoredWebUrl(prefs.getString(KEY_WEB, null))
        if (migrated != prefs.getString(KEY_WEB, null)) {
            prefs.edit().putString(KEY_WEB, migrated).apply()
        }
        return migrated
    }

    var themeMode by mutableStateOf(
        prefs.getString(KEY_THEME, DEFAULT_THEME) ?: DEFAULT_THEME
    )
        private set

    val isDarkTheme: Boolean
        get() = themeMode != THEME_LIGHT

    var reloadToken by mutableStateOf(0)
        private set

    fun setTheme(mode: String) {
        val normalized = if (mode == THEME_LIGHT) THEME_LIGHT else THEME_DARK
        if (themeMode == normalized) return
        themeMode = normalized
        prefs.edit().putString(KEY_THEME, normalized).apply()
    }

    fun updateUrls(api: String, web: String) {
        val apiTrim = api.trim().trimEnd('/')
        val webTrim = web.trim().trimEnd('/')
        apiServerUrl = apiTrim
        webAppUrl = webTrim
        prefs.edit()
            .putString(KEY_API, apiTrim)
            .putString(KEY_WEB, webTrim)
            .apply()
        reloadToken += 1
    }

    fun reload() {
        reloadToken += 1
    }

    companion object {
        private const val PREFS = "moonpointer_prefs"
        private const val KEY_API = "api_server_url"
        private const val KEY_WEB = "web_app_url"
        private const val KEY_THEME = "theme_mode"
        const val THEME_DARK = "dark"
        const val THEME_LIGHT = "light"
        const val DEFAULT_THEME = THEME_DARK
        const val DEFAULT_API = "http://10.0.2.2:8080"
        /** 内置 Web 包，无需电脑运行 npm run dev */
        const val DEFAULT_WEB = MoonPointerWebAssets.BUNDLED_WEB_URL
        const val DEV_WEB = "http://10.0.2.2:3000"
    }

    fun resetToDefaults() {
        updateUrls(DEFAULT_API, DEFAULT_WEB)
    }

    fun useDevWebServer() {
        updateUrls(apiServerUrl, DEV_WEB)
    }
}
