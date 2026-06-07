package com.englishai.translator.controller;

import com.englishai.translator.config.TranslatorProperties;
import com.englishai.translator.service.LlmTranslationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    private final LlmTranslationService translationService;
    private final TranslatorProperties properties;

    public HomeController(LlmTranslationService translationService, TranslatorProperties properties) {
        this.translationService = translationService;
        this.properties = properties;
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String home() {
        boolean ready = translationService.isConfigured();
        String provider = properties.getLlm().getProvider();
        String statusColor = ready ? "#22c55e" : "#ef4444";
        String statusText = ready
                ? provider + " API 已配置，可以传译"
                : "DeepSeek API 未配置，请编辑 application-local.yml";
        String webUrl = "http://localhost:3000";

        return """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>MoonPointer - 后端状态</title>
              <style>
                body { font-family: system-ui, sans-serif; background: #0a0a12; color: #f4f4f5;
                       display: flex; justify-content: center; align-items: center; min-height: 100vh; margin: 0; }
                .card { background: #16161f; border-radius: 16px; padding: 32px; max-width: 480px; width: 90%%; }
                h1 { font-size: 1.4rem; margin: 0 0 16px; }
                .status { display: flex; align-items: center; gap: 8px; margin-bottom: 20px; }
                .dot { width: 12px; height: 12px; border-radius: 50%%; background: %s; }
                a { color: #38bdf8; }
                .btn { display: inline-block; margin-top: 16px; padding: 12px 24px; background: #3b82f6;
                       color: white; text-decoration: none; border-radius: 8px; font-weight: 600; }
                code { background: #334155; padding: 2px 6px; border-radius: 4px; font-size: 0.85rem; }
              </style>
            </head>
            <body>
              <div class="card">
                <h1>MoonPointer</h1>
                <div class="status">
                  <div class="dot"></div>
                  <span>%s</span>
                </div>
                <p>后端服务运行正常 (端口 8080)</p>
                <p>WebSocket: <code>ws://localhost:8080/ws/translate</code></p>
                <p>健康检查 JSON: <a href="/api/health">/api/health</a></p>
                <a class="btn" href="%s">打开 Web 传译页面 →</a>
              </div>
            </body>
            </html>
            """.formatted(statusColor, statusText, webUrl);
    }
}
