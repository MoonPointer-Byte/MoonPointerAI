package com.englishai.translator.controller;

import com.englishai.translator.service.SttService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/stt")
public class SttController {

    private final SttService sttService;

    public SttController(SttService sttService) {
        this.sttService = sttService;
    }

    @PostMapping("/transcribe")
    public Map<String, String> transcribe(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "language", required = false) String language) throws Exception {
        String contentType = file.getContentType() != null ? file.getContentType() : "audio/webm";
        String text = sttService.transcribe(file.getBytes(), contentType, language);
        return Map.of("text", text);
    }
}
