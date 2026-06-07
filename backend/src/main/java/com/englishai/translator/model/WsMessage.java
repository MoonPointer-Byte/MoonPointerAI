package com.englishai.translator.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WsMessage {

    public enum Type {
        CONFIG,
        SPEECH,
        TRANSLATION,
        CORRECTION,
        ERROR,
        STATUS,
        PING,
        PONG
    }

    private Type type;
    private String sessionId;
    private String segmentId;
    private String text;
    private String translatedText;
    private String sourceLang;
    private String targetLang;
    private Boolean isFinal;
    private Boolean corrected;
    private String message;
    private Boolean ttsEnabled;

    public WsMessage() {}

    public static WsMessage of(Type type) {
        WsMessage msg = new WsMessage();
        msg.type = type;
        return msg;
    }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getSegmentId() { return segmentId; }
    public void setSegmentId(String segmentId) { this.segmentId = segmentId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getTranslatedText() { return translatedText; }
    public void setTranslatedText(String translatedText) { this.translatedText = translatedText; }

    public String getSourceLang() { return sourceLang; }
    public void setSourceLang(String sourceLang) { this.sourceLang = sourceLang; }

    public String getTargetLang() { return targetLang; }
    public void setTargetLang(String targetLang) { this.targetLang = targetLang; }

    public Boolean getIsFinal() { return isFinal; }
    public void setIsFinal(Boolean isFinal) { this.isFinal = isFinal; }

    public Boolean getCorrected() { return corrected; }
    public void setCorrected(Boolean corrected) { this.corrected = corrected; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Boolean getTtsEnabled() { return ttsEnabled; }
    public void setTtsEnabled(Boolean ttsEnabled) { this.ttsEnabled = ttsEnabled; }
}
