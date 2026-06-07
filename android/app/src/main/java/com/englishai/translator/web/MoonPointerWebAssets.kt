package com.englishai.translator.web

import android.content.Context
import androidx.webkit.WebViewAssetLoader

object MoonPointerWebAssets {
    /** HTTPS origin for bundled Web — avoids file:// ES module + JS bridge limits in WebView */
    const val BUNDLED_WEB_URL = "https://appassets.androidplatform.net/www/index.html"

    fun createAssetLoader(context: Context): WebViewAssetLoader =
        WebViewAssetLoader.Builder()
            .addPathHandler("/www/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()

    fun isBundledWebUrl(url: String): Boolean {
        val trimmed = url.trim()
        return trimmed == BUNDLED_WEB_URL ||
            trimmed.startsWith("https://appassets.androidplatform.net/www/") ||
            trimmed.startsWith("file:///android_asset/www/")
    }

    fun resolveLoadUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.startsWith("file:///android_asset/www/")) {
            trimmed.replace(
                "file:///android_asset/",
                "https://appassets.androidplatform.net/"
            )
        } else {
            trimmed
        }
    }

    fun migrateStoredWebUrl(url: String?): String {
        val value = url?.trim().orEmpty()
        return when {
            value.isEmpty() -> BUNDLED_WEB_URL
            value.startsWith("file:///android_asset/www/") -> BUNDLED_WEB_URL
            else -> value
        }
    }
}
