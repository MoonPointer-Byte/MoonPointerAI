package com.englishai.translator.dto;

public class NoteRequest {

    private String clientSessionId;
    private String videoUrl;
    private String title;
    private String content;
    private String manualContent;
    private String aiContent;
    private String preferredMode;
    private String sourceLang;
    private String targetLang;

    public String getClientSessionId() { return clientSessionId; }
    public void setClientSessionId(String clientSessionId) { this.clientSessionId = clientSessionId; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getManualContent() { return manualContent; }
    public void setManualContent(String manualContent) { this.manualContent = manualContent; }

    public String getAiContent() { return aiContent; }
    public void setAiContent(String aiContent) { this.aiContent = aiContent; }

    public String getPreferredMode() { return preferredMode; }
    public void setPreferredMode(String preferredMode) { this.preferredMode = preferredMode; }

    public String getSourceLang() { return sourceLang; }
    public void setSourceLang(String sourceLang) { this.sourceLang = sourceLang; }

    public String getTargetLang() { return targetLang; }
    public void setTargetLang(String targetLang) { this.targetLang = targetLang; }
}
