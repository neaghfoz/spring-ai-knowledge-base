package com.wantwant.sakb.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class ChatRequest {
    @NotBlank
    private String kbId;

    @NotBlank
    private String question;

    @Min(1)
    private int topK = 5;

    // optional conversation session id for multi-turn
    private String sessionId;

    public String getKbId() { return kbId; }
    public void setKbId(String kbId) { this.kbId = kbId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}
