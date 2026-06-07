package com.englishai.translator.speech

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

class TtsManager(context: Context) {
    private var tts: TextToSpeech? = null
    private var enabled = false
    private val queue = ConcurrentLinkedQueue<String>()
    private var speaking = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.SIMPLIFIED_CHINESE
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        speaking = false
                        speakNext()
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        speaking = false
                        speakNext()
                    }
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        speaking = false
                        speakNext()
                    }
                })
            }
        }
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) {
            queue.clear()
            tts?.stop()
            speaking = false
        }
    }

    fun speak(text: String) {
        if (!enabled || text.isBlank()) return
        queue.add(text)
        speakNext()
    }

    private fun speakNext() {
        if (!enabled || speaking || queue.isEmpty()) return
        val text = queue.poll() ?: return
        speaking = true
        val id = "tts_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), id)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
