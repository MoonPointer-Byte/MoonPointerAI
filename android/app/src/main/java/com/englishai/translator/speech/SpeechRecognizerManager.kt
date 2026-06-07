package com.englishai.translator.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.UUID

class SpeechRecognizerManager(
    private val context: Context,
    private val onResult: (text: String, isFinal: Boolean, segmentId: String) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null
    private var segmentId = UUID.randomUUID().toString()
    private var lang = "en-US"
    private var active = false

    fun setLanguage(sourceLang: String) {
        lang = when (sourceLang) {
            "en" -> "en-US"
            "ja" -> "ja-JP"
            "ko" -> "ko-KR"
            "fr" -> "fr-FR"
            "de" -> "de-DE"
            "es" -> "es-ES"
            else -> "en-US"
        }
    }

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return
        active = true
        createRecognizer()
        startListening()
    }

    fun stop() {
        active = false
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
    }

    private fun createRecognizer() {
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    if (active && error != SpeechRecognizer.ERROR_CLIENT) {
                        restartListening()
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim()
                    if (!text.isNullOrBlank()) {
                        onResult(text, true, segmentId)
                        segmentId = UUID.randomUUID().toString()
                    }
                    if (active) restartListening()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim()
                    if (!text.isNullOrBlank()) {
                        onResult(text, false, segmentId)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer?.startListening(intent)
    }

    private fun restartListening() {
        if (!active) return
        try {
            startListening()
        } catch (_: Exception) {
            createRecognizer()
            startListening()
        }
    }
}
