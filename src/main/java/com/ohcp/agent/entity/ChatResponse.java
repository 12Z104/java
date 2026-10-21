package com.ohcp.agent.entity;

import java.util.ArrayList;
import java.util.List;

/** 聊天响应：包含完整文本、候选缺素、结构化建议与检索来源 */
public class ChatResponse {

    private String sessionId;
    private String answer;
    private List<DeficiencyCandidate> candidates = new ArrayList<>();
    private FertilizerAdvice advice;
    private List<String> sources = new ArrayList<>();
    private long elapsedMs;
    private boolean llmEnhanced;
    private int knowledgeHits;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public List<DeficiencyCandidate> getCandidates() { return candidates; }
    public void setCandidates(List<DeficiencyCandidate> candidates) { this.candidates = candidates; }
    public FertilizerAdvice getAdvice() { return advice; }
    public void setAdvice(FertilizerAdvice advice) { this.advice = advice; }
    public List<String> getSources() { return sources; }
    public void setSources(List<String> sources) { this.sources = sources; }
    public long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(long elapsedMs) { this.elapsedMs = elapsedMs; }
    public boolean isLlmEnhanced() { return llmEnhanced; }
    public void setLlmEnhanced(boolean llmEnhanced) { this.llmEnhanced = llmEnhanced; }
    public int getKnowledgeHits() { return knowledgeHits; }
    public void setKnowledgeHits(int knowledgeHits) { this.knowledgeHits = knowledgeHits; }
}