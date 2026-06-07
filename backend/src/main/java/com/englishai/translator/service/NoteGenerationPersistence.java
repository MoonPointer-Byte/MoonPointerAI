package com.englishai.translator.service;

import com.englishai.translator.dto.NoteGenerateResponse;
import com.englishai.translator.entity.Note;
import com.englishai.translator.repository.NoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NoteGenerationPersistence {

    private final NoteRepository noteRepository;

    public NoteGenerationPersistence(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    @Transactional
    public Long markGenerating(String clientSessionId) {
        Note note = noteRepository.findFirstByClientSessionIdOrderByUpdatedAtDesc(clientSessionId)
                .orElseGet(() -> {
                    Note n = new Note();
                    n.setClientSessionId(clientSessionId);
                    n.setPreferredMode("AI");
                    n.setAiStatus("IDLE");
                    return n;
                });
        note.setAiStatus("GENERATING");
        if (note.getPreferredMode() == null) note.setPreferredMode("MANUAL");
        return noteRepository.save(note).getId();
    }

    @Transactional
    public NoteGenerateResponse saveSuccess(Long noteId, String aiContent, String sourceLang, String targetLang) {
        Note note = noteRepository.findById(noteId).orElseThrow();
        note.setAiContent(aiContent);
        note.setAiStatus("DONE");
        note.setLastAiGeneratedAt(LocalDateTime.now());
        if (sourceLang != null) note.setSourceLang(sourceLang);
        if (targetLang != null) note.setTargetLang(targetLang);
        noteRepository.save(note);

        NoteGenerateResponse response = new NoteGenerateResponse();
        response.setNoteId(noteId);
        response.setAiContent(aiContent);
        response.setAiStatus("DONE");
        response.setLastAiGeneratedAt(note.getLastAiGeneratedAt());
        return response;
    }

    @Transactional
    public void markFailed(Long noteId) {
        if (noteId == null) return;
        noteRepository.findById(noteId).ifPresent(note -> {
            note.setAiStatus("FAILED");
            noteRepository.save(note);
        });
    }
}
