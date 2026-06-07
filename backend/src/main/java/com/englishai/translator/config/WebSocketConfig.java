package com.englishai.translator.config;

import com.englishai.translator.websocket.TranslationWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TranslationWebSocketHandler translationHandler;

    public WebSocketConfig(TranslationWebSocketHandler translationHandler) {
        this.translationHandler = translationHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(translationHandler, "/ws/translate")
                .setAllowedOrigins("*");
    }
}
