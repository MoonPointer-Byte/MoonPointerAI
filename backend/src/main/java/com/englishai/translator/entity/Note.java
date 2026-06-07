package com.englishai.translator.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notes", indexes = @Index(name = "idx_client_session", columnList = "clientSessionId"))
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String clientSessionId;

    @Column(length = 512)
    private String videoUrl;

    @Column(length = 256)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String manualContent;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String aiContent;

    @Column(nullable = false, length = 16)
    private String preferredMode = "MANUAL";

    @Column(nullable = false, length = 16)
    private String aiStatus = "IDLE";

    private LocalDateTime lastAiGeneratedAt;

    @Column(length = 8)
    private String sourceLang;

    @Column(length = 8)
    private String targetLang;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public String getAiStatus() { return aiStatus; }
    public void setAiStatus(String aiStatus) { this.aiStatus = aiStatus; }

    public LocalDateTime getLastAiGeneratedAt() { return lastAiGeneratedAt; }
    public void setLastAiGeneratedAt(LocalDateTime lastAiGeneratedAt) { this.lastAiGeneratedAt = lastAiGeneratedAt; }

    public String getSourceLang() { return sourceLang; }
    public void setSourceLang(String sourceLang) { this.sourceLang = sourceLang; }

    public String getTargetLang() { return targetLang; }
    public void setTargetLang(String targetLang) { this.targetLang = targetLang; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
