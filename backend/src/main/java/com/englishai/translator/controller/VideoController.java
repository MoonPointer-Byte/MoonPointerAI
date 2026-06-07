package com.englishai.translator.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    private static final Logger log = LoggerFactory.getLogger(VideoController.class);
    private static final Pattern DOUYIN_VIDEO_ID = Pattern.compile("(?:video|note)/(\\d{10,})");
    private static final Pattern BILIBILI_BVID = Pattern.compile("(BV[0-9A-Za-z]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern YOUTUBE_ID = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?v=|embed/|shorts/)|youtu\\.be/)([\\w-]{11})");
    private static final Pattern URL_IN_TEXT = Pattern.compile("https?://[^\\s\\u4e00-\\u9fff]+");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @GetMapping("/resolve")
    public Map<String, String> resolve(@RequestParam(required = false) String url) {
        Map<String, String> result = new LinkedHashMap<>();
        if (url == null || url.isBlank()) {
            result.put("type", "unknown");
            result.put("message", "链接为空");
            return result;
        }

        String trimmed = extractUrl(url.trim());

        try {
            Matcher yt = YOUTUBE_ID.matcher(trimmed);
            if (yt.find()) {
                String id = yt.group(1);
                result.put("type", "youtube");
                result.put("videoId", id);
                result.put("embedUrl", "https://www.youtube.com/embed/" + id + "?rel=0");
                result.put("originalUrl", trimmed);
                return result;
            }

            Matcher bili = BILIBILI_BVID.matcher(trimmed);
            if (bili.find()) {
                String bvid = bili.group(1);
                result.put("type", "bilibili");
                result.put("videoId", bvid);
                result.put("embedUrl",
                        "https://player.bilibili.com/player.html?bvid=" + bvid + "&high_quality=1");
                result.put("originalUrl", trimmed);
                return result;
            }

            Matcher direct = DOUYIN_VIDEO_ID.matcher(trimmed);
            if (direct.find()) {
                return douyinResult(direct.group(1), trimmed);
            }

            if (isDouyinHost(trimmed)) {
                return resolveDouyinShortLink(trimmed);
            }

            if (trimmed.matches("(?i)^https?://.*\\.(mp4|webm|ogg)(\\?.*)?$")) {
                result.put("type", "direct");
                result.put("src", trimmed);
                result.put("originalUrl", trimmed);
                return result;
            }

            result.put("type", "unknown");
            result.put("originalUrl", trimmed);
            result.put("message", "无法识别链接，请尝试完整视频地址");
            return result;
        } catch (Exception e) {
            log.warn("Video resolve failed for {}: {}", trimmed, e.getMessage());
            result.put("type", "link");
            result.put("originalUrl", trimmed);
            result.put("message", "解析失败，请直接在浏览器打开原链接播放");
            return result;
        }
    }

    private Map<String, String> resolveDouyinShortLink(String trimmed) {
        Map<String, String> fallback = new LinkedHashMap<>();
        fallback.put("type", "douyin");
        fallback.put("originalUrl", trimmed);
        fallback.put("message", "短链接无法自动解析，请点击在抖音打开");

        try {
            URI uri = URI.create(trimmed);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0")
                    .header("Accept", "text/html,application/xhtml+xml")
                    .GET()
                    .timeout(Duration.ofSeconds(6))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String finalUrl = response.uri().toString();
            String body = response.body() == null ? "" : response.body();

            Matcher fromUrl = DOUYIN_VIDEO_ID.matcher(finalUrl);
            if (fromUrl.find()) {
                return douyinResult(fromUrl.group(1), finalUrl);
            }

            Matcher fromBody = DOUYIN_VIDEO_ID.matcher(body);
            if (fromBody.find()) {
                return douyinResult(fromBody.group(1), finalUrl);
            }

            fallback.put("originalUrl", finalUrl);
            return fallback;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid douyin URL: {}", trimmed);
            return fallback;
        } catch (Exception e) {
            log.warn("Douyin fetch failed: {}", e.getMessage());
            return fallback;
        }
    }

    private boolean isDouyinHost(String url) {
        String lower = url.toLowerCase();
        return lower.contains("douyin.com") || lower.contains("iesdouyin.com");
    }

    private String extractUrl(String input) {
        Matcher m = URL_IN_TEXT.matcher(input);
        if (m.find()) {
            return m.group().replaceAll("[.,;:!?）)]+$", "");
        }
        return input;
    }

    private Map<String, String> douyinResult(String id, String originalUrl) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("type", "douyin");
        result.put("videoId", id);
        result.put("embedUrl", "https://www.douyin.com/light/" + id);
        result.put("originalUrl", originalUrl);
        return result;
    }
}
