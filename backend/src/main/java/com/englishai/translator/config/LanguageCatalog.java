package com.englishai.translator.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LanguageCatalog {

    public record Language(String code, String label, String speechTag) {}

    private static final List<Language> LANGUAGES = List.of(
            new Language("zh", "中文", "zh-CN"),
            new Language("en", "英语", "en-US"),
            new Language("ja", "日语", "ja-JP"),
            new Language("ko", "韩语", "ko-KR"),
            new Language("fr", "法语", "fr-FR"),
            new Language("de", "德语", "de-DE"),
            new Language("es", "西班牙语", "es-ES"),
            new Language("ru", "俄语", "ru-RU"),
            new Language("pt", "葡萄牙语", "pt-BR"),
            new Language("it", "意大利语", "it-IT"),
            new Language("ar", "阿拉伯语", "ar-SA"),
            new Language("th", "泰语", "th-TH"),
            new Language("vi", "越南语", "vi-VN"),
            new Language("id", "印尼语", "id-ID"),
            new Language("ms", "马来语", "ms-MY"),
            new Language("hi", "印地语", "hi-IN"),
            new Language("nl", "荷兰语", "nl-NL"),
            new Language("pl", "波兰语", "pl-PL"),
            new Language("tr", "土耳其语", "tr-TR"),
            new Language("sv", "瑞典语", "sv-SE")
    );

    private LanguageCatalog() {}

    public static List<Language> all() {
        return LANGUAGES;
    }

    public static Map<String, Object> toApiResponse() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", LANGUAGES.stream().map(Language::code).toList());
        result.put("target", LANGUAGES.stream().map(Language::code).toList());
        result.put("labels", LANGUAGES.stream().collect(LinkedHashMap::new,
                (m, l) -> m.put(l.code(), l.label()), LinkedHashMap::putAll));
        result.put("speechTags", LANGUAGES.stream().collect(LinkedHashMap::new,
                (m, l) -> m.put(l.code(), l.speechTag()), LinkedHashMap::putAll));
        return result;
    }

    public static String speechTag(String code) {
        return LANGUAGES.stream()
                .filter(l -> l.code().equals(code))
                .map(Language::speechTag)
                .findFirst()
                .orElse("en-US");
    }
}
