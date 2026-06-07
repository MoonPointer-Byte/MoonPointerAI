package com.englishai.translator.notes;

/**
 * LangGraph 风格工作流状态：在多个 LLM 节点之间传递上下文。
 */
public class NoteWorkflowState {

    private final String transcript;
    private final String sourceLang;
    private final String targetLang;
    private String summary;
    private String vocabulary;
    private String keySentences;
    private String finalNote;

    public NoteWorkflowState(String transcript, String sourceLang, String targetLang) {
        this.transcript = transcript;
        this.sourceLang = sourceLang;
        this.targetLang = targetLang;
    }

    public String getTranscript() { return transcript; }
    public String getSourceLang() { return sourceLang; }
    public String getTargetLang() { return targetLang; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getVocabulary() { return vocabulary; }
    public void setVocabulary(String vocabulary) { this.vocabulary = vocabulary; }

    public String getKeySentences() { return keySentences; }
    public void setKeySentences(String keySentences) { this.keySentences = keySentences; }

    public String getFinalNote() { return finalNote; }
    public void setFinalNote(String finalNote) { this.finalNote = finalNote; }
}
