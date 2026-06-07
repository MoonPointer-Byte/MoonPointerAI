package com.englishai.translator.repository;

import com.englishai.translator.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    Optional<Note> findFirstByClientSessionIdOrderByUpdatedAtDesc(String clientSessionId);

    List<Note> findByClientSessionIdOrderByUpdatedAtDesc(String clientSessionId);
}
