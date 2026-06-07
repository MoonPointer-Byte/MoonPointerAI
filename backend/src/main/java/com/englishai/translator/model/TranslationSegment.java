package com.englishai.translator.model;

import java.time.Instant;

public class TranslationSegment {

    private String id;
    private String sourceText;
    private String translatedText;
    private boolean isFinal;
    private boolean corrected;
    private String sourceLang;
    private String targetLang;
    private Instant timestamp;

    public TranslationSegment() {
        this.timestamp = Instant.now();
    }

    public TranslationSegment(String id, String sourceText, String translatedText, boolean isFinal) {
        this();
        this.id = id;
        this.sourceText = sourceText;
        this.translatedText = translatedText;
        this.isFinal = isFinal;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSourceText() { return sourceText; }
    public void setSourceText(String sourceText) { this.sourceText = sourceText; }

    public String getTranslatedText() { return translatedText; }
    public void setTranslatedText(String translatedText) { this.translatedText = translatedText; }

    public boolean isFinal() { return isFinal; }
    public void setFinal(boolean aFinal) { isFinal = aFinal; }

    public boolean isCorrected() { return corrected; }
    public void setCorrected(boolean corrected) { this.corrected = corrected; }

    public String getSourceLang() { return sourceLang; }
    public void setSourceLang(String sourceLang) { this.sourceLang = sourceLang; }

    public String getTargetLang() { return targetLang; }
    public void setTargetLang(String targetLang) { this.targetLang = targetLang; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
