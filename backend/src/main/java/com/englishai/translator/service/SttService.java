package com.englishai.translator.service;

import com.englishai.translator.config.TranslatorProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@Service
public class SttService {

    private static final Logger log = LoggerFactory.getLogger(SttService.class);

    private final TranslatorProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public SttService(TranslatorProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public boolean isConfigured() {
        TranslatorProperties.Stt stt = properties.getStt();
        return stt.isEnabled()
                && stt.getApiKey() != null
                && !stt.getApiKey().isBlank();
    }

    public String transcribe(byte[] audioBytes, String contentType, String language) {
        if (!isConfigured()) {
            throw new IllegalStateException("语音识别 API 未配置，请在 application-local.yml 配置 translator.stt.api-key（国内推荐硅基流动 cloud.siliconflow.cn）");
        }
        if (audioBytes == null || audioBytes.length < 1000) {
            return "";
        }

        TranslatorProperties.Stt stt = properties.getStt();
        try {
            String boundary = "----EnglishAI" + UUID.randomUUID();
            byte[] body = buildMultipart(boundary, audioBytes, contentType, stt.getModel(), language);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(stt.getApiUrl()))
                    .timeout(Duration.ofSeconds(45))
                    .header("Authorization", "Bearer " + stt.getApiKey())
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("STT API error {}: {}", response.statusCode(), response.body());
                throw new IllegalStateException(parseApiError(response.statusCode(), response.body()));
            }

            JsonNode root = objectMapper.readTree(response.body());
            String text = cleanSenseVoiceText(root.path("text").asText("").trim());
            return text;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("STT transcribe failed", e);
            throw new IllegalStateException("语音识别请求失败: " + e.getMessage());
        }
    }

    /** SenseVoice 会附带 <|ja|><|NEUTRAL|><|Speech|> 等标签，需清洗后再翻译 */
    private String cleanSenseVoiceText(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String text = raw.replaceAll("<\\|[^|]+\\|>", "").trim();
        text = text.replaceAll("[\\p{So}\\p{Sk}]", "").trim();
        text = text.replaceAll("\\s{2,}", " ");
        return text;
    }

    private String parseApiError(int status, String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String msg = root.path("message").asText("");
            if (msg.isBlank()) msg = root.path("error").path("message").asText("");
            if (msg.toLowerCase().contains("balance") || msg.contains("余额")) {
                return "硅基流动账户余额不足，请前往 cloud.siliconflow.cn 充值后再试";
            }
            if (!msg.isBlank()) return "语音识别失败: " + msg;
        } catch (Exception ignored) { /* use fallback */ }
        if (status == 403) return "语音识别被拒绝 (403)，请检查 API Key 或账户余额";
        if (status == 401) return "语音识别 API Key 无效";
        if (status == 500) {
            return "语音识别服务暂时不可用 (500)，可能是音频格式不兼容或硅基流动服务端异常，请稍后重试";
        }
        return "语音识别失败: HTTP " + status;
    }

    private byte[] buildMultipart(String boundary, byte[] audio, String contentType,
                                  String model, String language) {
        String mime = contentType != null && !contentType.isBlank() ? contentType : "audio/webm";
        String ext = mime.contains("ogg") ? "ogg" : "webm";
        StringBuilder sb = new StringBuilder();
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"audio.").append(ext).append("\"\r\n");
        sb.append("Content-Type: ").append(mime).append("\r\n\r\n");

        byte[] header = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] modelPart = (
                "\r\n--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"model\"\r\n\r\n" +
                model
        ).getBytes(StandardCharsets.UTF_8);

        byte[] langPart = language != null && !language.isBlank()
                ? (
                "\r\n--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"language\"\r\n\r\n" +
                language
        ).getBytes(StandardCharsets.UTF_8)
                : new byte[0];

        byte[] end = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

        byte[] body = new byte[header.length + audio.length + modelPart.length + langPart.length + end.length];
        int pos = 0;
        System.arraycopy(header, 0, body, pos, header.length);
        pos += header.length;
        System.arraycopy(audio, 0, body, pos, audio.length);
        pos += audio.length;
        System.arraycopy(modelPart, 0, body, pos, modelPart.length);
        pos += modelPart.length;
        if (langPart.length > 0) {
            System.arraycopy(langPart, 0, body, pos, langPart.length);
            pos += langPart.length;
        }
        System.arraycopy(end, 0, body, pos, end.length);
        return body;
    }
}
