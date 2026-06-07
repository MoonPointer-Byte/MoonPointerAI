package com.englishai.translator.bridge

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import com.englishai.translator.speech.AndroidTtsController

class MoonPointerAndroidBridge(
    private val apiServerUrl: () -> String,
    private val webAppUrl: () -> String,
    private val themeMode: () -> String,
    private val onThemeChange: (String) -> Unit,
    private val ttsController: AndroidTtsController,
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
) {
    @JavascriptInterface
    fun getServerUrl(): String = apiServerUrl()

    @JavascriptInterface
    fun getWebUrl(): String = webAppUrl()

    @JavascriptInterface
    fun isAndroidApp(): Boolean = true

    @JavascriptInterface
    fun getTheme(): String = themeMode()

    @JavascriptInterface
    fun setTheme(mode: String) {
        mainHandler.post { onThemeChange(mode) }
    }

    @JavascriptInterface
    fun speakTts(text: String, lang: String, rate: Double, utteranceId: String) {
        mainHandler.post {
            ttsController.speak(text, lang, rate.toFloat(), utteranceId)
        }
    }

    @JavascriptInterface
    fun stopTts() {
        mainHandler.post { ttsController.stop() }
    }
}
