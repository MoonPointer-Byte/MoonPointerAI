package com.englishai.translator.service;

import com.englishai.translator.model.TranslationSegment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TranslationSession {

    private final String sessionId;
    private String sourceLang = "en";
    private String targetLang = "zh";
    private boolean ttsEnabled = false;
    private final Map<String, TranslationSegment> segments = new LinkedHashMap<>();
    private final int maxSegments;

    public TranslationSession(int maxSegments) {
        this.sessionId = UUID.randomUUID().toString();
        this.maxSegments = maxSegments;
    }

    public String getSessionId() { return sessionId; }

    public String getSourceLang() { return sourceLang; }
    public void setSourceLang(String sourceLang) { this.sourceLang = sourceLang; }

    public String getTargetLang() { return targetLang; }
    public void setTargetLang(String targetLang) { this.targetLang = targetLang; }

    public boolean isTtsEnabled() { return ttsEnabled; }
    public void setTtsEnabled(boolean ttsEnabled) { this.ttsEnabled = ttsEnabled; }

    public TranslationSegment upsertSegment(String segmentId, String sourceText, String translatedText, boolean isFinal) {
        TranslationSegment segment = segments.get(segmentId);
        if (segment == null) {
            segment = new TranslationSegment(segmentId, sourceText, translatedText, isFinal);
            segment.setSourceLang(sourceLang);
            segment.setTargetLang(targetLang);
            segments.put(segmentId, segment);
            trimSegments();
        } else {
            segment.setSourceText(sourceText);
            segment.setTranslatedText(translatedText);
            segment.setFinal(isFinal);
        }
        return segment;
    }

    public void correctSegment(String segmentId, String correctedTranslation) {
        TranslationSegment segment = segments.get(segmentId);
        if (segment != null) {
            segment.setTranslatedText(correctedTranslation);
            segment.setCorrected(true);
        }
    }

    public TranslationSegment getSegment(String segmentId) {
        return segments.get(segmentId);
    }

    public List<TranslationSegment> getRecentSegments(int count) {
        List<TranslationSegment> all = new ArrayList<>(segments.values());
        int start = Math.max(0, all.size() - count);
        return all.subList(start, all.size());
    }

    public List<TranslationSegment> getAllSegments() {
        return new ArrayList<>(segments.values());
    }

    private void trimSegments() {
        while (segments.size() > maxSegments) {
            String firstKey = segments.keySet().iterator().next();
            segments.remove(firstKey);
        }
    }
}
