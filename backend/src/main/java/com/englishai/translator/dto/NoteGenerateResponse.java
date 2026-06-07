package com.englishai.translator.dto;

import java.time.LocalDateTime;

public class NoteGenerateResponse {

    private Long noteId;
    private String aiContent;
    private String aiStatus;
    private LocalDateTime lastAiGeneratedAt;
    private String message;

    public Long getNoteId() { return noteId; }
    public void setNoteId(Long noteId) { this.noteId = noteId; }

    public String getAiContent() { return aiContent; }
    public void setAiContent(String aiContent) { this.aiContent = aiContent; }

    public String getAiStatus() { return aiStatus; }
    public void setAiStatus(String aiStatus) { this.aiStatus = aiStatus; }

    public LocalDateTime getLastAiGeneratedAt() { return lastAiGeneratedAt; }
    public void setLastAiGeneratedAt(LocalDateTime lastAiGeneratedAt) { this.lastAiGeneratedAt = lastAiGeneratedAt; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
