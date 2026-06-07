package com.englishai.translator.config;

import com.englishai.translator.service.LlmTranslationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupConfigLogger {

    private static final Logger log = LoggerFactory.getLogger(StartupConfigLogger.class);

    private final TranslatorProperties properties;
    private final LlmTranslationService translationService;

    public StartupConfigLogger(TranslatorProperties properties, LlmTranslationService translationService) {
        this.properties = properties;
        this.translationService = translationService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logConfig() {
        String key = properties.getLlm().getApiKey();
        String model = properties.getLlm().getModel();
        String keyPreview = (key != null && key.length() > 8)
                ? key.substring(0, 8) + "..."
                : "(未设置)";
        String provider = properties.getLlm().getProvider();
        log.info("{} 配置 — API Key: {}, Model: {}, 已就绪: {}",
                provider, keyPreview,
                model == null || model.isBlank() ? "(未设置)" : model,
                translationService.isConfigured());
        if (!translationService.isConfigured()) {
            log.warn("翻译服务未就绪！请在 backend/application-local.yml 中配置 DeepSeek api-key，然后重启后端。");
        }
    }
}
