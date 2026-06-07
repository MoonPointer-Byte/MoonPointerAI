package com.englishai.translator.model

import com.google.gson.annotations.SerializedName

data class WsMessage(
    val type: String,
    @SerializedName("sessionId") val sessionId: String? = null,
    @SerializedName("segmentId") val segmentId: String? = null,
    val text: String? = null,
    @SerializedName("translatedText") val translatedText: String? = null,
    @SerializedName("sourceLang") val sourceLang: String? = null,
    @SerializedName("targetLang") val targetLang: String? = null,
    @SerializedName("isFinal") val isFinal: Boolean? = null,
    val corrected: Boolean? = null,
    val message: String? = null,
    @SerializedName("ttsEnabled") val ttsEnabled: Boolean? = null
)

data class SubtitleSegment(
    val id: String,
    val sourceText: String,
    val translatedText: String,
    val isFinal: Boolean = false,
    val corrected: Boolean = false
)
