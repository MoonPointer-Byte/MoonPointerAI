package com.englishai.translator.websocket;

import com.englishai.translator.config.TranslatorProperties;
import com.englishai.translator.model.TranslationSegment;
import com.englishai.translator.model.WsMessage;
import com.englishai.translator.service.LlmTranslationService;
import com.englishai.translator.service.TranslationSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

@Component
public class TranslationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(TranslationWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final LlmTranslationService translationService;
    private final TranslatorProperties properties;
    private final Map<String, TranslationSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, ExecutorService> sessionExecutors = new ConcurrentHashMap<>();
    private final Map<String, Object> sendLocks = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> segmentVersions = new ConcurrentHashMap<>();

    public TranslationWebSocketHandler(ObjectMapper objectMapper,
                                       LlmTranslationService translationService,
                                       TranslatorProperties properties) {
        this.objectMapper = objectMapper;
        this.translationService = translationService;
        this.properties = properties;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession wsSession) throws Exception {
        TranslationSession session = new TranslationSession(properties.getSession().getMaxContextSegments());
        sessions.put(wsSession.getId(), session);
        sessionExecutors.put(wsSession.getId(), Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "translate-" + wsSession.getId());
            t.setDaemon(true);
            return t;
        }));

        WsMessage status = WsMessage.of(WsMessage.Type.STATUS);
        status.setSessionId(session.getSessionId());
        status.setMessage(translationService.isConfigured() ? "connected" : "connected_no_llm");
        send(wsSession, status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession wsSession, TextMessage message) throws Exception {
        WsMessage incoming = objectMapper.readValue(message.getPayload(), WsMessage.class);
        TranslationSession session = sessions.get(wsSession.getId());
        if (session == null) {
            sendError(wsSession, "Session not found");
            return;
        }

        switch (incoming.getType()) {
            case PING -> send(wsSession, WsMessage.of(WsMessage.Type.PONG));
            case CONFIG -> handleConfig(wsSession, session, incoming);
            case SPEECH -> handleSpeech(wsSession, session, incoming);
            default -> sendError(wsSession, "Unknown message type: " + incoming.getType());
        }
    }

    private void handleConfig(WebSocketSession wsSession, TranslationSession session, WsMessage msg) throws IOException {
        if (msg.getSourceLang() != null) session.setSourceLang(msg.getSourceLang());
        if (msg.getTargetLang() != null) session.setTargetLang(msg.getTargetLang());
        if (msg.getTtsEnabled() != null) session.setTtsEnabled(msg.getTtsEnabled());

        WsMessage status = WsMessage.of(WsMessage.Type.STATUS);
        status.setSessionId(session.getSessionId());
        status.setMessage("configured");
        status.setSourceLang(session.getSourceLang());
        status.setTargetLang(session.getTargetLang());
        status.setTtsEnabled(session.isTtsEnabled());
        send(wsSession, status);
    }

    private void handleSpeech(WebSocketSession wsSession, TranslationSession session, WsMessage msg) {
        String text = msg.getText();
        if (text == null || text.isBlank()) return;

        String segmentId = msg.getSegmentId() != null ? msg.getSegmentId() : UUID.randomUUID().toString();
        boolean isFinal = Boolean.TRUE.equals(msg.getIsFinal());
        long version = segmentVersions
                .computeIfAbsent(segmentKey(wsSession.getId(), segmentId), k -> new AtomicLong(0))
                .incrementAndGet();

        ExecutorService executor = sessionExecutors.get(wsSession.getId());
        if (executor == null) return;

        BooleanSupplier cancelled = () -> !isCurrentVersion(wsSession.getId(), segmentId, version);

        executor.submit(() -> {
            try {
                if (isFinal) {
                    handleFinalTranslation(wsSession, session, segmentId, text, version, cancelled);
                } else {
                    handleInterimTranslation(wsSession, session, segmentId, text, version, cancelled);
                }
            } catch (Exception e) {
                if (cancelled.getAsBoolean()) return;
                log.error("Translation error", e);
                try {
                    sendError(wsSession, "Translation failed: " + e.getMessage());
                } catch (IOException ex) {
                    log.error("Failed to send error", ex);
                }
            }
        });
    }

    private void handleInterimTranslation(WebSocketSession wsSession, TranslationSession session,
                                          String segmentId, String text, long version,
                                          BooleanSupplier cancelled) throws IOException {
        String cached = translationService.getCached(text, session.getSourceLang(), session.getTargetLang());
        if (cached != null && !cached.isBlank()) {
            if (cancelled.getAsBoolean()) return;
            session.upsertSegment(segmentId, text, cached, false);
            sendTranslation(wsSession, session, segmentId, text, cached, false);
            return;
        }

        AtomicReference<String> lastSent = new AtomicReference<>("");

        String translated = translationService.translateStream(
                text,
                session.getSourceLang(),
                session.getTargetLang(),
                List.of(),
                true,
                partial -> {
                    if (cancelled.getAsBoolean()) return;
                    if (partial.equals(lastSent.get())) return;
                    lastSent.set(partial);
                    try {
                        session.upsertSegment(segmentId, text, partial, false);
                        sendTranslation(wsSession, session, segmentId, text, partial, false);
                    } catch (IOException e) {
                        log.error("Failed to send partial translation", e);
                    }
                },
                cancelled);

        if (cancelled.getAsBoolean()) return;

        session.upsertSegment(segmentId, text, translated, false);
        if (!translated.equals(lastSent.get())) {
            sendTranslation(wsSession, session, segmentId, text, translated, false);
        }
    }

    private void handleFinalTranslation(WebSocketSession wsSession, TranslationSession session,
                                        String segmentId, String text, long version,
                                        BooleanSupplier cancelled) throws IOException {
        String cached = translationService.getCached(text, session.getSourceLang(), session.getTargetLang());
        if (cached != null && !cached.isBlank()) {
            if (cancelled.getAsBoolean()) return;
            TranslationSegment segment = session.upsertSegment(segmentId, text, cached, true);
            sendTranslation(wsSession, session, segment.getId(), segment.getSourceText(),
                    segment.getTranslatedText(), true);
            segmentVersions.remove(segmentKey(wsSession.getId(), segmentId));
            return;
        }

        TranslationSegment existing = session.getSegment(segmentId);
        if (existing != null && existing.getTranslatedText() != null && !existing.getTranslatedText().isBlank()
                && text.trim().equals(existing.getSourceText().trim())) {
            if (cancelled.getAsBoolean()) return;
            TranslationSegment segment = session.upsertSegment(segmentId, text, existing.getTranslatedText(), true);
            sendTranslation(wsSession, session, segment.getId(), segment.getSourceText(),
                    segment.getTranslatedText(), true);
            segmentVersions.remove(segmentKey(wsSession.getId(), segmentId));
            return;
        }

        AtomicReference<String> lastSent = new AtomicReference<>("");

        String translated = translationService.translateStream(
                text,
                session.getSourceLang(),
                session.getTargetLang(),
                List.of(),
                true,
                partial -> {
                    if (cancelled.getAsBoolean()) return;
                    if (partial.equals(lastSent.get())) return;
                    lastSent.set(partial);
                    try {
                        session.upsertSegment(segmentId, text, partial, false);
                        sendTranslation(wsSession, session, segmentId, text, partial, false);
                    } catch (IOException e) {
                        log.error("Failed to send partial translation", e);
                    }
                },
                cancelled);

        if (cancelled.getAsBoolean()) return;

        if (translated == null || translated.isBlank()) {
            sendError(wsSession, "翻译服务繁忙，请检查 DeepSeek API 余额或稍后重试");
            sendTranslation(wsSession, session, segmentId, text, "", false);
            return;
        }

        TranslationSegment segment = session.upsertSegment(segmentId, text, translated, true);
        sendTranslation(wsSession, session, segment.getId(), segment.getSourceText(),
                segment.getTranslatedText(), true);

        segmentVersions.remove(segmentKey(wsSession.getId(), segmentId));

        if (properties.getSession().isCorrectionEnabled()) {
            runCorrection(wsSession, session);
        }
    }

    private boolean isCurrentVersion(String wsId, String segmentId, long version) {
        AtomicLong current = segmentVersions.get(segmentKey(wsId, segmentId));
        return current != null && current.get() == version;
    }

    private String segmentKey(String wsId, String segmentId) {
        return wsId + ":" + segmentId;
    }

    private void sendTranslation(WebSocketSession wsSession, TranslationSession session,
                                 String segmentId, String sourceText, String translatedText,
                                 boolean isFinal) throws IOException {
        WsMessage response = WsMessage.of(WsMessage.Type.TRANSLATION);
        response.setSessionId(session.getSessionId());
        response.setSegmentId(segmentId);
        response.setText(sourceText);
        response.setTranslatedText(translatedText);
        response.setSourceLang(session.getSourceLang());
        response.setTargetLang(session.getTargetLang());
        response.setIsFinal(isFinal);
        response.setCorrected(false);
        send(wsSession, response);
    }

    private void runCorrection(WebSocketSession wsSession, TranslationSession session) {
        int window = properties.getSession().getCorrectionWindow();
        List<TranslationSegment> recent = session.getRecentSegments(window);
        if (recent.size() < 2) return;

        Map<String, String> corrections = translationService.correctPrevious(
                recent, session.getSourceLang(), session.getTargetLang());

        for (Map.Entry<String, String> entry : corrections.entrySet()) {
            session.correctSegment(entry.getKey(), entry.getValue());
            try {
                WsMessage correction = WsMessage.of(WsMessage.Type.CORRECTION);
                correction.setSessionId(session.getSessionId());
                correction.setSegmentId(entry.getKey());
                correction.setTranslatedText(entry.getValue());
                correction.setCorrected(true);
                send(wsSession, correction);
            } catch (IOException e) {
                log.error("Failed to send correction", e);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession wsSession, CloseStatus status) {
        sessions.remove(wsSession.getId());
        sendLocks.remove(wsSession.getId());
        segmentVersions.keySet().removeIf(k -> k.startsWith(wsSession.getId() + ":"));
        ExecutorService executor = sessionExecutors.remove(wsSession.getId());
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private void send(WebSocketSession wsSession, WsMessage msg) throws IOException {
        Object lock = sendLocks.computeIfAbsent(wsSession.getId(), k -> new Object());
        synchronized (lock) {
            if (wsSession.isOpen()) {
                wsSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
            }
        }
    }

    private void sendError(WebSocketSession wsSession, String error) throws IOException {
        WsMessage msg = WsMessage.of(WsMessage.Type.ERROR);
        msg.setMessage(error);
        send(wsSession, msg);
    }
}
