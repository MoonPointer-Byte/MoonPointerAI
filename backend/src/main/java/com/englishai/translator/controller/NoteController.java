package com.englishai.translator.controller;

import com.englishai.translator.dto.*;
import com.englishai.translator.service.NoteGenerationService;
import com.englishai.translator.service.NoteService;
import com.englishai.translator.service.TranscriptSegmentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;
    private final TranscriptSegmentService transcriptSegmentService;
    private final NoteGenerationService noteGenerationService;

    public NoteController(NoteService noteService,
                          TranscriptSegmentService transcriptSegmentService,
                          NoteGenerationService noteGenerationService) {
        this.noteService = noteService;
        this.transcriptSegmentService = transcriptSegmentService;
        this.noteGenerationService = noteGenerationService;
    }

    @GetMapping("/current")
    public NoteDto getCurrent(@RequestParam String clientSessionId) {
        return noteService.getCurrent(clientSessionId);
    }

    @GetMapping
    public List<NoteDto> list(@RequestParam String clientSessionId) {
        return noteService.list(clientSessionId);
    }

    @PutMapping
    public NoteDto save(@RequestBody NoteRequest request) {
        return noteService.save(request);
    }

    @PostMapping("/segments/sync")
    public Map<String, Object> syncSegments(@RequestBody SegmentSyncRequest request) {
        int synced = transcriptSegmentService.sync(request);
        return Map.of("status", "ok", "synced", synced);
    }

    @PostMapping("/generate")
    public NoteGenerateResponse generate(@RequestBody NoteGenerateRequest request) {
        return noteGenerationService.generate(request);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        noteService.delete(id);
        return Map.of("status", "ok");
    }
}
