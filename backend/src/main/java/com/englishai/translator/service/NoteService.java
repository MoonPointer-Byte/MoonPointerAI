package com.englishai.translator.service;

import com.englishai.translator.dto.NoteDto;
import com.englishai.translator.dto.NoteRequest;
import com.englishai.translator.entity.Note;
import com.englishai.translator.repository.NoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NoteService {

    private final NoteRepository noteRepository;

    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    @Transactional(readOnly = true)
    public NoteDto getCurrent(String clientSessionId) {
        return noteRepository.findFirstByClientSessionIdOrderByUpdatedAtDesc(clientSessionId)
                .map(NoteDto::from)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<NoteDto> list(String clientSessionId) {
        return noteRepository.findByClientSessionIdOrderByUpdatedAtDesc(clientSessionId)
                .stream()
                .map(NoteDto::from)
                .toList();
    }

    @Transactional
    public NoteDto save(NoteRequest request) {
        if (request.getClientSessionId() == null || request.getClientSessionId().isBlank()) {
            throw new IllegalArgumentException("clientSessionId is required");
        }

        Note note = noteRepository.findFirstByClientSessionIdOrderByUpdatedAtDesc(request.getClientSessionId())
                .orElseGet(Note::new);

        note.setClientSessionId(request.getClientSessionId());
        if (request.getVideoUrl() != null) note.setVideoUrl(request.getVideoUrl());
        if (request.getTitle() != null) note.setTitle(request.getTitle());

        if (request.getManualContent() != null) {
            note.setManualContent(request.getManualContent());
            note.setContent(request.getManualContent());
        } else if (request.getContent() != null) {
            note.setManualContent(request.getContent());
            note.setContent(request.getContent());
        }

        if (request.getAiContent() != null) note.setAiContent(request.getAiContent());
        if (request.getPreferredMode() != null) note.setPreferredMode(request.getPreferredMode().toUpperCase());
        if (request.getSourceLang() != null) note.setSourceLang(request.getSourceLang());
        if (request.getTargetLang() != null) note.setTargetLang(request.getTargetLang());

        if (note.getPreferredMode() == null) note.setPreferredMode("MANUAL");
        if (note.getAiStatus() == null) note.setAiStatus("IDLE");

        return NoteDto.from(noteRepository.save(note));
    }

    @Transactional
    public void delete(Long id) {
        noteRepository.deleteById(id);
    }
}
