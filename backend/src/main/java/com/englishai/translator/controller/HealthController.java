package com.englishai.translator.controller;

import com.englishai.translator.config.TranslatorProperties;
import com.englishai.translator.repository.NoteRepository;
import com.englishai.translator.service.LlmTranslationService;
import com.englishai.translator.service.SttService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final LlmTranslationService translationService;
    private final TranslatorProperties properties;
    private final NoteRepository noteRepository;
    private final SttService sttService;

    public HealthController(LlmTranslationService translationService,
                            TranslatorProperties properties,
                            NoteRepository noteRepository,
                            SttService sttService) {
        this.translationService = translationService;
        this.properties = properties;
        this.noteRepository = noteRepository;
        this.sttService = sttService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        boolean dbReady = checkDatabase();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", dbReady ? "ok" : "degraded");
        result.put("dbReady", dbReady);
        result.put("llmConfigured", translationService.isConfigured());
        result.put("provider", properties.getLlm().getProvider());
        result.put("model", properties.getLlm().getModel());
        result.put("notesPipeline", "langchain4j");
        result.put("sttConfigured", sttService.isConfigured());
        return result;
    }

    private boolean checkDatabase() {
        try {
            noteRepository.count();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @GetMapping("/languages")
    public Map<String, Object> languages() {
        return com.englishai.translator.config.LanguageCatalog.toApiResponse();
    }
}
