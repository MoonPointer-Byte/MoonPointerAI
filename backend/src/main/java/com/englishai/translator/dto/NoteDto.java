package com.englishai.translator.dto;

import com.englishai.translator.entity.Note;

import java.time.LocalDateTime;

public class NoteDto {

    private Long id;
    private String clientSessionId;
    private String videoUrl;
    private String title;
    private String content;
    private String manualContent;
    private String aiContent;
    private String preferredMode;
    private String aiStatus;
    private String sourceLang;
    private String targetLang;
    private LocalDateTime lastAiGeneratedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static NoteDto from(Note note) {
        NoteDto dto = new NoteDto();
        dto.id = note.getId();
        dto.clientSessionId = note.getClientSessionId();
        dto.videoUrl = note.getVideoUrl();
        dto.title = note.getTitle();
        String manual = note.getManualContent();
        if (manual == null || manual.isBlank()) {
            manual = note.getContent();
        }
        dto.manualContent = manual;
        dto.content = manual;
        dto.aiContent = note.getAiContent();
        dto.preferredMode = note.getPreferredMode() != null ? note.getPreferredMode() : "MANUAL";
        dto.aiStatus = note.getAiStatus() != null ? note.getAiStatus() : "IDLE";
        dto.sourceLang = note.getSourceLang();
        dto.targetLang = note.getTargetLang();
        dto.lastAiGeneratedAt = note.getLastAiGeneratedAt();
        dto.createdAt = note.getCreatedAt();
        dto.updatedAt = note.getUpdatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getClientSessionId() { return clientSessionId; }
    public String getVideoUrl() { return videoUrl; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getManualContent() { return manualContent; }
    public String getAiContent() { return aiContent; }
    public String getPreferredMode() { return preferredMode; }
    public String getAiStatus() { return aiStatus; }
    public String getSourceLang() { return sourceLang; }
    public String getTargetLang() { return targetLang; }
    public LocalDateTime getLastAiGeneratedAt() { return lastAiGeneratedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
