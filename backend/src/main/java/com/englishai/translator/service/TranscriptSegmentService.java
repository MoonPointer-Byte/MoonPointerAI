package com.englishai.translator.service;

import com.englishai.translator.dto.SegmentSyncRequest;
import com.englishai.translator.entity.TranscriptSegmentEntity;
import com.englishai.translator.repository.TranscriptSegmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TranscriptSegmentService {

    private final TranscriptSegmentRepository repository;

    public TranscriptSegmentService(TranscriptSegmentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public int sync(SegmentSyncRequest request) {
        if (request.getClientSessionId() == null || request.getClientSessionId().isBlank()) {
            throw new IllegalArgumentException("clientSessionId is required");
        }
        int count = 0;
        for (SegmentSyncRequest.SegmentItem item : request.getSegments()) {
            if (item.getSegmentId() == null || item.getSourceText() == null || item.getSourceText().isBlank()) {
                continue;
            }
            TranscriptSegmentEntity entity = repository
                    .findByClientSessionIdAndSegmentId(request.getClientSessionId(), item.getSegmentId())
                    .orElseGet(TranscriptSegmentEntity::new);

            entity.setClientSessionId(request.getClientSessionId());
            entity.setSegmentId(item.getSegmentId());
            entity.setSourceText(item.getSourceText());
            if (item.getTranslatedText() != null) {
                entity.setTranslatedText(item.getTranslatedText());
            }
            if (item.getTimestampMs() != null) {
                entity.setTimestampMs(item.getTimestampMs());
            }
            entity.setFinal(item.isFinal());
            repository.save(entity);
            count++;
        }
        return count;
    }

    @Transactional(readOnly = true)
    public String buildTranscriptText(String clientSessionId) {
        List<TranscriptSegmentEntity> segments =
                repository.findByClientSessionIdOrderByTimestampMsAsc(clientSessionId);

        return segments.stream()
                .filter(TranscriptSegmentEntity::isFinal)
                .map(seg -> {
                    String src = seg.getSourceText() != null ? seg.getSourceText().trim() : "";
                    String tgt = seg.getTranslatedText() != null ? seg.getTranslatedText().trim() : "";
                    if (tgt.isBlank()) return src;
                    return src + " → " + tgt;
                })
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining("\n"));
    }

    @Transactional(readOnly = true)
    public int countFinalSegments(String clientSessionId) {
        return (int) repository.findByClientSessionIdOrderByTimestampMsAsc(clientSessionId)
                .stream()
                .filter(TranscriptSegmentEntity::isFinal)
                .count();
    }
}
