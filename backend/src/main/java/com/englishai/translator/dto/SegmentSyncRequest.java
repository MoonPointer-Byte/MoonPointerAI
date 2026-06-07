package com.englishai.translator.dto;

import java.util.ArrayList;
import java.util.List;

public class SegmentSyncRequest {

    private String clientSessionId;
    private List<SegmentItem> segments = new ArrayList<>();

    public String getClientSessionId() { return clientSessionId; }
    public void setClientSessionId(String clientSessionId) { this.clientSessionId = clientSessionId; }

    public List<SegmentItem> getSegments() { return segments; }
    public void setSegments(List<SegmentItem> segments) { this.segments = segments; }

    public static class SegmentItem {
        private String segmentId;
        private String sourceText;
        private String translatedText;
        private Long timestampMs;
        private boolean isFinal = true;

        public String getSegmentId() { return segmentId; }
        public void setSegmentId(String segmentId) { this.segmentId = segmentId; }

        public String getSourceText() { return sourceText; }
        public void setSourceText(String sourceText) { this.sourceText = sourceText; }

        public String getTranslatedText() { return translatedText; }
        public void setTranslatedText(String translatedText) { this.translatedText = translatedText; }

        public Long getTimestampMs() { return timestampMs; }
        public void setTimestampMs(Long timestampMs) { this.timestampMs = timestampMs; }

        public boolean isFinal() { return isFinal; }
        public void setFinal(boolean aFinal) { isFinal = aFinal; }
    }
}
