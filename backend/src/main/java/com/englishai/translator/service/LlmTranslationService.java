package com.englishai.translator.service;

import com.englishai.translator.config.TranslatorProperties;
import com.englishai.translator.model.TranslationSegment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LlmTranslationService {

    private static final Logger log = LoggerFactory.getLogger(LlmTranslationService.class);
    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");

    private final TranslatorProperties properties;
    private final TranslationCache cache;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public LlmTranslationService(TranslatorProperties properties,
                                 TranslationCache cache,
                                 ObjectMapper objectMapper) {
        this.properties = properties;
        this.cache = cache;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .version(java.net.http.HttpClient.Version.HTTP_2)
                .build();
    }

    public boolean isConfigured() {
        String key = properties.getLlm().getApiKey();
        String model = properties.getLlm().getModel();
        return key != null && !key.isBlank()
                && model != null && !model.isBlank();
    }

    public String translate(String text, String sourceLang, String targetLang, List<TranslationSegment> context) {
        return translate(text, sourceLang, targetLang, context, false);
    }

    public String translateFast(String text, String sourceLang, String targetLang) {
        return translate(text, sourceLang, targetLang, List.of(), true);
    }

    public String getCached(String text, String sourceLang, String targetLang) {
        return cache.get(sourceLang, targetLang, text);
    }

    public String translateStream(String text, String sourceLang, String targetLang,
                                  List<TranslationSegment> context, boolean fast,
                                  Consumer<String> onPartial) {
        return translateStream(text, sourceLang, targetLang, context, fast, onPartial, null);
    }

    public String translateStream(String text, String sourceLang, String targetLang,
                                  List<TranslationSegment> context, boolean fast,
                                  Consumer<String> onPartial, BooleanSupplier cancelled) {
        if (!isConfigured()) {
            return "";
        }

        String cached = cache.get(sourceLang, targetLang, text);
        if (cached != null) {
            onPartial.accept(cached);
            return cached;
        }

        String systemPrompt = fast
                ? buildFastSystemPrompt(sourceLang, targetLang)
                : buildSystemPrompt(sourceLang, targetLang);
        String userPrompt = fast
                ? buildFastTranslatePrompt(text, context)
                : (context.isEmpty() ? text : buildTranslatePrompt(text, context));

        try {
            String result = callLlmStream(systemPrompt, userPrompt, fast, onPartial, cancelled);
            boolean aborted = cancelled != null && cancelled.getAsBoolean();
            if (!aborted && result != null && !result.isBlank()) {
                cache.put(sourceLang, targetLang, text, result);
            }
            return result != null ? result : "";
        } catch (Exception e) {
            if (cancelled != null && cancelled.getAsBoolean()) {
                return "";
            }
            log.warn("LLM stream translation failed: {}", e.getMessage());
            return "";
        }
    }

    private String translate(String text, String sourceLang, String targetLang,
                             List<TranslationSegment> context, boolean fast) {
        if (!isConfigured()) {
            return "";
        }

        String cached = cache.get(sourceLang, targetLang, text);
        if (cached != null) {
            return cached;
        }

        String systemPrompt = fast
                ? buildFastSystemPrompt(sourceLang, targetLang)
                : buildSystemPrompt(sourceLang, targetLang);
        String userPrompt = fast
                ? buildFastTranslatePrompt(text, context)
                : (context.isEmpty() ? text : buildTranslatePrompt(text, context));

        try {
            String response = fast
                    ? callLlm(systemPrompt, userPrompt, true)
                    : callLlmWithRetry(systemPrompt, userPrompt, false);
            String result = extractTranslation(response);
            if (result != null && !result.isBlank()) {
                cache.put(sourceLang, targetLang, text, result);
            }
            return result;
        } catch (Exception e) {
            log.warn("LLM translation failed: {}", e.getMessage());
            return "";
        }
    }

    public Map<String, String> correctPrevious(List<TranslationSegment> segments, String sourceLang, String targetLang) {
        if (!isConfigured() || segments.size() < 2) {
            return Map.of();
        }

        String systemPrompt = buildCorrectionSystemPrompt(sourceLang, targetLang);
        String userPrompt = buildCorrectionPrompt(segments);

        try {
            String response = callLlm(systemPrompt, userPrompt);
            return parseCorrections(response);
        } catch (Exception e) {
            log.warn("LLM correction failed: {}", e.getMessage());
            return Map.of();
        }
    }

    private String buildSystemPrompt(String sourceLang, String targetLang) {
        return buildStrictSystemPrompt(sourceLang, targetLang, false);
    }

    private String buildFastSystemPrompt(String sourceLang, String targetLang) {
        return buildStrictSystemPrompt(sourceLang, targetLang, true);
    }

    private String buildStrictSystemPrompt(String sourceLang, String targetLang, boolean fast) {
        if (!properties.getLlm().isStrictTranslationOnly()) {
            return String.format(
                "将%s译为%s。只输出译文。",
                langName(sourceLang), langName(targetLang));
        }
        String src = langName(sourceLang);
        String tgt = langName(targetLang);
        if (fast) {
            return "你是翻译引擎。将" + src + "译为" + tgt + "。"
                    + "只输出译文一行。禁止解释、标签、markdown、引号、原文复述。";
        }
        return "你是专业同声传译翻译引擎，不是聊天助手。\n"
                + "任务：将" + src + "准确译为" + tgt + "。\n"
                + "硬性规则：\n"
                + "1. 只输出译文，禁止任何解释、注释、前后缀\n"
                + "2. 禁止输出「翻译」「译文」「Translation」「注：」等标签\n"
                + "3. 禁止 markdown、代码块、引号包裹、括号说明\n"
                + "4. 禁止复述或重复原文\n"
                + "5. 忠实原意，口语自然，不增删不臆测";
    }

    private String buildFastTranslatePrompt(String text, List<TranslationSegment> context) {
        if (context.isEmpty() || !properties.getLlm().isStrictTranslationOnly()) {
            return text;
        }
        return text;
    }

    private String buildCorrectionSystemPrompt(String sourceLang, String targetLang) {
        return String.format(
            "You are a simultaneous interpretation quality reviewer. Given recent speech segments in %s and their %s translations, " +
            "identify segments where the translation should be corrected based on fuller context. " +
            "Respond ONLY with a JSON array: [{\"id\":\"segment_id\",\"translation\":\"corrected text\"}]. " +
            "Only include segments that need correction. If none, respond with [].",
            langName(sourceLang), langName(targetLang)
        );
    }

    private String buildTranslatePrompt(String text, List<TranslationSegment> context) {
        if (properties.getLlm().isStrictTranslationOnly()) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        if (!context.isEmpty()) {
            sb.append("Recent context:\n");
            for (TranslationSegment seg : context) {
                sb.append("- ").append(seg.getSourceText()).append(" → ").append(seg.getTranslatedText()).append("\n");
            }
            sb.append("\n");
        }
        sb.append("Translate: ").append(text);
        return sb.toString();
    }

    private String buildCorrectionPrompt(List<TranslationSegment> segments) {
        StringBuilder sb = new StringBuilder("Segments:\n");
        for (TranslationSegment seg : segments) {
            sb.append(String.format("{\"id\":\"%s\",\"source\":\"%s\",\"translation\":\"%s\"}\n",
                    seg.getId(),
                    escapeJson(seg.getSourceText()),
                    escapeJson(seg.getTranslatedText())));
        }
        return sb.toString();
    }

    private String callLlmStream(String systemPrompt, String userPrompt, boolean fast,
                                 Consumer<String> onPartial) throws Exception {
        return callLlmStream(systemPrompt, userPrompt, fast, onPartial, null);
    }

    private String callLlmStream(String systemPrompt, String userPrompt, boolean fast,
                                 Consumer<String> onPartial, BooleanSupplier cancelled) throws Exception {
        ObjectNode body = buildRequestBody(systemPrompt, userPrompt, fast);
        body.put("stream", true);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getLlm().getApiUrl()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + properties.getLlm().getApiKey())
                .timeout(Duration.ofSeconds(fast ? 5 : 25))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<java.io.InputStream> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            String err = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("LLM API error: " + response.statusCode() + " " + err);
        }

        StringBuilder full = new StringBuilder();
        try (java.io.InputStream raw = response.body();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(raw, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (cancelled != null && cancelled.getAsBoolean()) {
                    raw.close();
                    break;
                }
                if (!line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) break;
                JsonNode root = objectMapper.readTree(data);
                String delta = root.path("choices").path(0).path("delta").path("content").asText("");
                if (!delta.isEmpty()) {
                    full.append(delta);
                    onPartial.accept(full.toString());
                }
            }
        }
        return extractTranslation(full.toString());
    }

    private ObjectNode buildRequestBody(String systemPrompt, String userPrompt, boolean fast) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.getLlm().getModel());
        body.put("temperature", 0);
        if (fast) {
            body.put("max_tokens", 64);
        } else {
            body.put("max_tokens", 256);
        }
        ArrayNode messages = body.putArray("messages");
        ObjectNode system = messages.addObject();
        system.put("role", "system");
        system.put("content", systemPrompt);
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", userPrompt);
        return body;
    }

    private String callLlmWithRetry(String systemPrompt, String userPrompt, boolean fast) throws Exception {
        int maxAttempts = 3;
        Exception last = null;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                return callLlm(systemPrompt, userPrompt, fast);
            } catch (Exception e) {
                last = e;
                if (!isRateLimited(e) || attempt == maxAttempts - 1) {
                    throw e;
                }
                long delayMs = 500L * (attempt + 1);
                log.info("Rate limited, retrying in {}ms (attempt {})", delayMs, attempt + 2);
                Thread.sleep(delayMs);
            }
        }
        throw last;
    }

    private boolean isRateLimited(Exception e) {
        String msg = e.getMessage();
        return msg != null && (msg.contains("429") || msg.contains("TooManyRequests")
                || msg.contains("SetLimitExceeded"));
    }

    private String callLlm(String systemPrompt, String userPrompt) throws Exception {
        return callLlm(systemPrompt, userPrompt, false);
    }

    private String callLlm(String systemPrompt, String userPrompt, boolean fast) throws Exception {
        ObjectNode body = buildRequestBody(systemPrompt, userPrompt, fast);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getLlm().getApiUrl()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + properties.getLlm().getApiKey())
                .timeout(Duration.ofSeconds(fast ? 5 : 20))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("LLM API error: " + response.statusCode() + " " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        return root.path("choices").path(0).path("message").path("content").asText("");
    }

    private String extractTranslation(String response) {
        if (response == null) return "";
        String cleaned = response.trim();
        if (cleaned.isEmpty()) return "";

        Matcher codeBlock = JSON_BLOCK.matcher(cleaned);
        if (codeBlock.find()) {
            cleaned = codeBlock.group(1).trim();
        }

        cleaned = cleaned.replaceAll("^```[\\w]*\\s*", "").replaceAll("\\s*```$", "");

        String[] lines = cleaned.split("\\r?\\n");
        if (lines.length > 1 && properties.getLlm().isStrictTranslationOnly()) {
            cleaned = lines[0].trim();
        }

        cleaned = cleaned
                .replaceFirst("^(翻译|译文|中文翻译|英文翻译|Translation|Translated|Output)\\s*[:：]\\s*", "")
                .replaceFirst("^[\"'「『](.*)[\"'」』]$", "$1")
                .trim();

        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() > 1) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    private Map<String, String> parseCorrections(String response) {
        Map<String, String> corrections = new HashMap<>();
        try {
            String json = response.trim();
            Matcher m = JSON_BLOCK.matcher(json);
            if (m.find()) {
                json = m.group(1).trim();
            }
            JsonNode array = objectMapper.readTree(json);
            if (array.isArray()) {
                for (JsonNode node : array) {
                    String id = node.path("id").asText(null);
                    String translation = node.path("translation").asText(null);
                    if (id != null && translation != null && !translation.isBlank()) {
                        corrections.put(id, translation);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse corrections: {}", e.getMessage());
        }
        return corrections;
    }

    private String langName(String code) {
        return switch (code) {
            case "en" -> "English";
            case "zh" -> "Chinese";
            case "ja" -> "Japanese";
            case "ko" -> "Korean";
            case "fr" -> "French";
            case "de" -> "German";
            case "es" -> "Spanish";
            case "ru" -> "Russian";
            case "pt" -> "Portuguese";
            case "it" -> "Italian";
            case "ar" -> "Arabic";
            case "th" -> "Thai";
            case "vi" -> "Vietnamese";
            case "id" -> "Indonesian";
            case "ms" -> "Malay";
            case "hi" -> "Hindi";
            case "nl" -> "Dutch";
            case "pl" -> "Polish";
            case "tr" -> "Turkish";
            case "sv" -> "Swedish";
            default -> code;
        };
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
