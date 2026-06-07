package com.englishai.translator.service;

import com.englishai.translator.config.TranslatorProperties;
import com.englishai.translator.dto.NoteGenerateRequest;
import com.englishai.translator.dto.NoteGenerateResponse;
import com.englishai.translator.entity.Note;
import com.englishai.translator.notes.NoteGenerationWorkflow;
import com.englishai.translator.repository.NoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NoteGenerationService {

    private static final Logger log = LoggerFactory.getLogger(NoteGenerationService.class);

    private final NoteRepository noteRepository;
    private final TranscriptSegmentService transcriptSegmentService;
    private final NoteGenerationWorkflow workflow;
    private final NoteGenerationPersistence persistence;
    private final TranslatorProperties properties;

    public NoteGenerationService(NoteRepository noteRepository,
                                 TranscriptSegmentService transcriptSegmentService,
                                 NoteGenerationWorkflow workflow,
                                 NoteGenerationPersistence persistence,
                                 TranslatorProperties properties) {
        this.noteRepository = noteRepository;
        this.transcriptSegmentService = transcriptSegmentService;
        this.workflow = workflow;
        this.persistence = persistence;
        this.properties = properties;
    }

    public NoteGenerateResponse generate(NoteGenerateRequest request) {
        if (request.getClientSessionId() == null || request.getClientSessionId().isBlank()) {
            throw new IllegalArgumentException("clientSessionId is required");
        }
        if (properties.getLlm().getApiKey() == null || properties.getLlm().getApiKey().isBlank()) {
            throw new IllegalStateException("DeepSeek API 未配置，无法生成 AI 笔记");
        }

        Long noteId = persistence.markGenerating(request.getClientSessionId());

        try {
            String transcript = transcriptSegmentService.buildTranscriptText(request.getClientSessionId());
            if (transcript.isBlank()) {
                throw new IllegalArgumentException("暂无字幕内容，请先开始传译并等待字幕同步");
            }

            Note note = noteRepository.findById(noteId).orElseThrow();
            String sourceLang = request.getSourceLang() != null ? request.getSourceLang() : note.getSourceLang();
            String targetLang = request.getTargetLang() != null ? request.getTargetLang() : note.getTargetLang();

            String aiContent = workflow.run(transcript, sourceLang, targetLang);
            return persistence.saveSuccess(noteId, aiContent, sourceLang, targetLang);
        } catch (RuntimeException e) {
            log.error("AI note generation failed: {}", e.getMessage());
            persistence.markFailed(noteId);
            throw e;
        }
    }
}
