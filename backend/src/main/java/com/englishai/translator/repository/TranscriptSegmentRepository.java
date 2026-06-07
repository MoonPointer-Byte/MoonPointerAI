package com.englishai.translator.repository;

import com.englishai.translator.entity.TranscriptSegmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TranscriptSegmentRepository extends JpaRepository<TranscriptSegmentEntity, Long> {

    List<TranscriptSegmentEntity> findByClientSessionIdOrderByTimestampMsAsc(String clientSessionId);

    Optional<TranscriptSegmentEntity> findByClientSessionIdAndSegmentId(String clientSessionId, String segmentId);

    void deleteByClientSessionId(String clientSessionId);
}
