package com.englishai.translator.network

import android.util.Log
import com.englishai.translator.model.WsMessage
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class TranslationWebSocket(
    private val serverUrl: String,
    private val onMessage: (WsMessage) -> Unit,
    private val onConnectionChange: (Boolean) -> Unit
) {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null

    fun connect() {
        val wsUrl = serverUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .trimEnd('/') + "/ws/translate"

        val request = Request.Builder().url(wsUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onConnectionChange(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val msg = gson.fromJson(text, WsMessage::class.java)
                    onMessage(msg)
                } catch (e: Exception) {
                    Log.e(TAG, "Parse error", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                onConnectionChange(false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                onConnectionChange(false)
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "User stopped")
        webSocket = null
        onConnectionChange(false)
    }

    fun send(msg: WsMessage) {
        webSocket?.send(gson.toJson(msg))
    }

    fun isConnected(): Boolean = webSocket != null

    companion object {
        private const val TAG = "TranslationWS"
    }
}
