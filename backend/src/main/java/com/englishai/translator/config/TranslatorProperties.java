package com.englishai.translator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "translator")
public class TranslatorProperties {

    private Llm llm = new Llm();
    private Stt stt = new Stt();
    private Session session = new Session();

    public Llm getLlm() { return llm; }
    public void setLlm(Llm llm) { this.llm = llm; }

    public Stt getStt() { return stt; }
    public void setStt(Stt stt) { this.stt = stt; }

    public Session getSession() { return session; }
    public void setSession(Session session) { this.session = session; }

    public static class Llm {
        private String provider = "deepseek";
        private String apiUrl = "https://api.deepseek.com/chat/completions";
        private String apiKey = "";
        private String model = "deepseek-chat";
        /** 严格模式：仅输出译文，禁止解释、标签、markdown */
        private boolean strictTranslationOnly = true;

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public String getApiUrl() { return apiUrl; }
        public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public boolean isStrictTranslationOnly() { return strictTranslationOnly; }
        public void setStrictTranslationOnly(boolean strictTranslationOnly) {
            this.strictTranslationOnly = strictTranslationOnly;
        }
    }

    public static class Stt {
        private boolean enabled = true;
        private String apiUrl = "https://api.siliconflow.cn/v1/audio/transcriptions";
        private String apiKey = "";
        private String model = "FunAudioLLM/SenseVoiceSmall";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getApiUrl() { return apiUrl; }
        public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }

    public static class Session {
        private int maxContextSegments = 20;
        private int correctionWindow = 5;
        private boolean correctionEnabled = false;

        public int getMaxContextSegments() { return maxContextSegments; }
        public void setMaxContextSegments(int maxContextSegments) { this.maxContextSegments = maxContextSegments; }

        public int getCorrectionWindow() { return correctionWindow; }
        public void setCorrectionWindow(int correctionWindow) { this.correctionWindow = correctionWindow; }

        public boolean isCorrectionEnabled() { return correctionEnabled; }
        public void setCorrectionEnabled(boolean correctionEnabled) { this.correctionEnabled = correctionEnabled; }
    }
}
