package com.englishai.translator.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "transcript_segments",
    indexes = {
        @Index(name = "idx_seg_session", columnList = "clientSessionId"),
        @Index(name = "idx_seg_time", columnList = "clientSessionId,timestampMs")
    },
    uniqueConstraints = @UniqueConstraint(
        name = "uk_session_segment",
        columnNames = {"clientSessionId", "segmentId"}
    )
)
public class TranscriptSegmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String clientSessionId;

    @Column(nullable = false, length = 64)
    private String segmentId;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String sourceText;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String translatedText;

    private Long timestampMs;

    @Column(nullable = false)
    private boolean isFinal = true;

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
