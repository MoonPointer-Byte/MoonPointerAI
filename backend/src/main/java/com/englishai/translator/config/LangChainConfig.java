package com.englishai.translator.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LangChainConfig {

    @Bean
    public ChatLanguageModel noteChatModel(TranslatorProperties properties) {
        TranslatorProperties.Llm llm = properties.getLlm();
        String baseUrl = llm.getApiUrl()
                .replace("/chat/completions", "")
                .replaceAll("/$", "");
        if (!baseUrl.endsWith("/v1")) {
            baseUrl = baseUrl + "/v1";
        }

        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(llm.getApiKey() != null ? llm.getApiKey() : "")
                .modelName(llm.getModel())
                .temperature(0.3)
                .timeout(Duration.ofSeconds(60))
                .maxTokens(1200)
                .logRequests(false)
                .logResponses(false)
                .build();
    }
}
