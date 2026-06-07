package com.englishai.translator.notes;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LangGraph 风格笔记流水线（LangChain4j）：单次 LLM 调用生成完整结构化笔记。
 */
@Component
public class NoteGenerationWorkflow {

    private static final Logger log = LoggerFactory.getLogger(NoteGenerationWorkflow.class);
    private static final int MAX_TRANSCRIPT_CHARS = 12000;

    private final ChatLanguageModel chatModel;

    public NoteGenerationWorkflow(ChatLanguageModel noteChatModel) {
        this.chatModel = noteChatModel;
    }

    public String run(String rawTranscript, String sourceLang, String targetLang) {
        String transcript = trimTranscript(rawTranscript);
        if (transcript.isBlank()) {
            throw new IllegalArgumentException("暂无字幕内容，请先开始传译");
        }

        String src = langLabel(sourceLang);
        String tgt = langLabel(targetLang);

        String systemPrompt = "你是专业的语言学习笔记助手。"
                + "根据用户提供的双语字幕（" + src + " → " + tgt + "），生成结构化学习笔记。"
                + "必须包含以下三个部分，使用 markdown 二级标题："
                + "\n## 内容概要\n（3-5句" + tgt + "概要）"
                + "\n## 重点词汇\n（5-10个，每行：原文 | " + tgt + "释义）"
                + "\n## 精彩句型\n（3-5句，每行：原文 → " + tgt + "翻译）"
                + "\n只输出笔记正文，禁止额外解释。";

        String result = chatModel.chat(ChatRequest.builder()
                .messages(List.of(
                        SystemMessage.from(systemPrompt),
                        UserMessage.from("双语字幕：\n" + transcript)
                ))
                .build())
                .aiMessage()
                .text();

        String note = result != null ? result.trim() : "";
        if (note.isBlank()) {
            throw new IllegalStateException("AI 未返回有效笔记内容，请稍后重试");
        }
        log.info("Note workflow completed, {} chars", note.length());
        return note;
    }

    private String langLabel(String code) {
        if (code == null) return "原文";
        return switch (code.toLowerCase()) {
            case "en" -> "英语";
            case "ja" -> "日语";
            case "ko" -> "韩语";
            case "zh" -> "中文";
            case "fr" -> "法语";
            case "de" -> "德语";
            case "es" -> "西班牙语";
            default -> code;
        };
    }

    private String trimTranscript(String raw) {
        if (raw == null) return "";
        String t = raw.trim();
        if (t.length() <= MAX_TRANSCRIPT_CHARS) return t;
        return t.substring(t.length() - MAX_TRANSCRIPT_CHARS);
    }
}
