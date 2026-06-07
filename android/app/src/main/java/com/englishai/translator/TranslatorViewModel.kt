package com.englishai.translator

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.englishai.translator.model.SubtitleSegment
import com.englishai.translator.model.WsMessage
import com.englishai.translator.network.TranslationWebSocket
import com.englishai.translator.speech.SpeechRecognizerManager
import com.englishai.translator.speech.TtsManager

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {

    var serverUrl by mutableStateOf("http://10.0.2.2:8080")
    var sourceLang by mutableStateOf("en")
    var targetLang by mutableStateOf("zh")
    var ttsEnabled by mutableStateOf(false)
    var showSource by mutableStateOf(true)
    var showSettings by mutableStateOf(false)

    var connected by mutableStateOf(false)
    var active by mutableStateOf(false)
    var statusText by mutableStateOf("未连接")
    var interimSource by mutableStateOf("")

    val segments = mutableStateListOf<SubtitleSegment>()

    private var webSocket: TranslationWebSocket? = null
    private var speechManager: SpeechRecognizerManager? = null
    private val ttsManager = TtsManager(application)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastSpoken = ""

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block()
        else mainHandler.post(block)
    }

    fun start() {
        webSocket = TranslationWebSocket(
            serverUrl = serverUrl,
            onMessage = ::handleWsMessage,
            onConnectionChange = { isConnected ->
                runOnMain {
                    connected = isConnected
                    statusText = if (isConnected) "已连接" else "未连接"
                }
            }
        )
        webSocket?.connect()

        speechManager = SpeechRecognizerManager(getApplication()) { text, isFinal, segmentId ->
            runOnMain { interimSource = if (isFinal) "" else text }
            webSocket?.send(
                WsMessage(
                    type = "SPEECH",
                    segmentId = segmentId,
                    text = text,
                    isFinal = isFinal,
                    sourceLang = sourceLang,
                    targetLang = targetLang
                )
            )
        }
        speechManager?.setLanguage(sourceLang)

        segments.clear()
        interimSource = ""
        lastSpoken = ""
        active = true

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            webSocket?.send(
                WsMessage(
                    type = "CONFIG",
                    sourceLang = sourceLang,
                    targetLang = targetLang,
                    ttsEnabled = ttsEnabled
                )
            )
            speechManager?.start()
        }, 500)
    }

    fun stop() {
        active = false
        speechManager?.stop()
        speechManager = null
        webSocket?.disconnect()
        webSocket = null
        ttsManager.setEnabled(false)
        statusText = "已停止"
    }

    fun updateTts(enabled: Boolean) {
        ttsEnabled = enabled
        ttsManager.setEnabled(enabled)
    }

    private fun handleWsMessage(msg: WsMessage) {
        runOnMain {
            when (msg.type) {
                "STATUS" -> {
                    statusText = if (msg.message == "connected_no_llm") "已连接（未配置 LLM）" else "已连接"
                }
                "TRANSLATION" -> {
                    val id = msg.segmentId ?: return@runOnMain
                    val seg = SubtitleSegment(
                        id = id,
                        sourceText = msg.text ?: "",
                        translatedText = msg.translatedText ?: "",
                        isFinal = msg.isFinal ?: false
                    )
                    val idx = segments.indexOfFirst { it.id == id }
                    if (idx >= 0) segments[idx] = seg else segments.add(seg)

                    if (msg.isFinal == true && seg.translatedText.isNotBlank() && seg.translatedText != lastSpoken) {
                        lastSpoken = seg.translatedText
                        ttsManager.speak(seg.translatedText)
                    }
                }
                "CORRECTION" -> {
                    val id = msg.segmentId ?: return@runOnMain
                    val idx = segments.indexOfFirst { it.id == id }
                    if (idx >= 0) {
                        segments[idx] = segments[idx].copy(
                            translatedText = msg.translatedText ?: segments[idx].translatedText,
                            corrected = true
                        )
                    }
                }
                "ERROR" -> {
                    statusText = "错误: ${msg.message}"
                }
            }
        }
    }

    override fun onCleared() {
        stop()
        ttsManager.shutdown()
        super.onCleared()
    }
}
