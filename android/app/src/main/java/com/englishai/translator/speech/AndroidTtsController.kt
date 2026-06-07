package com.englishai.translator.speech

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

private data class TtsRequest(
    val text: String,
    val langTag: String,
    val rate: Float,
    val utteranceId: String
)

class AndroidTtsController(context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false
    private val queue = ConcurrentLinkedQueue<TtsRequest>()
    private var speaking = false
    var onUtteranceDone: (String) -> Unit = {}

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        finish(utteranceId)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        finish(utteranceId)
                    }
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        finish(utteranceId)
                    }
                })
                drainQueue()
            }
        }
    }

    private fun finish(utteranceId: String?) {
        speaking = false
        if (utteranceId != null) {
            onUtteranceDone(utteranceId)
        }
        drainQueue()
    }

    fun speak(text: String, langTag: String, rate: Float, utteranceId: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        queue.add(
            TtsRequest(
                text = trimmed,
                langTag = langTag,
                rate = rate.coerceIn(0.5f, 2.0f),
                utteranceId = utteranceId
            )
        )
        drainQueue()
    }

    fun stop() {
        val pending = queue.toList()
        queue.clear()
        speaking = false
        tts?.stop()
        for (req in pending) {
            onUtteranceDone(req.utteranceId)
        }
    }

    private fun drainQueue() {
        if (!ready || speaking || queue.isEmpty()) return
        val req = queue.poll() ?: return
        val engine = tts ?: return

        val locale = localeFromTag(req.langTag)
        if (engine.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
            engine.language = locale
        }
        engine.setSpeechRate(req.rate)

        speaking = true
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }
        engine.speak(req.text, TextToSpeech.QUEUE_FLUSH, params, req.utteranceId)
    }

    private fun localeFromTag(tag: String): Locale {
        val parts = tag.split('-', '_')
        return when (parts.size) {
            1 -> Locale(parts[0])
            2 -> Locale(parts[0], parts[1])
            else -> Locale.forLanguageTag(tag)
        }
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
