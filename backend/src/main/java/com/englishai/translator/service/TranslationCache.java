package com.englishai.translator.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TranslationCache {

    private static final int MAX_SIZE = 2000;

    private final Map<String, String> cache = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > MAX_SIZE;
        }
    };

    public String get(String sourceLang, String targetLang, String text) {
        return cache.get(key(sourceLang, targetLang, text));
    }

    public void put(String sourceLang, String targetLang, String text, String translation) {
        if (text == null || translation == null || text.isBlank() || translation.isBlank()) return;
        cache.put(key(sourceLang, targetLang, text), translation);
    }

    private String key(String sourceLang, String targetLang, String text) {
        return sourceLang + "|" + targetLang + "|" + text.trim().toLowerCase();
    }
}
