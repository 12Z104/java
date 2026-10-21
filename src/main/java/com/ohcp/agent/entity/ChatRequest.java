package com.ohcp.agent.entity;

import java.util.List;

/** 聊天请求：既支持整句自然语言，也支持可选结构化字段辅助解析 */
public class ChatRequest {

    private String sessionId;
    private String message;

    private String crop;
    private String part;
    private String symptom;
    private String environment;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getCrop() { return crop; }
    public void setCrop(String crop) { this.crop = crop; }
    public String getPart() { return part; }
    public void setPart(String part) { this.part = part; }
    public String getSymptom() { return symptom; }
    public void setSymptom(String symptom) { this.symptom = symptom; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    /** 将结构化字段拼接到整句描述中，便于统一检索 */
    public String buildQueryText() {
        StringBuilder sb = new StringBuilder();
        if (message != null && !message.isBlank()) sb.append(message).append(" ");
        if (crop != null && !crop.isBlank()) sb.append("作物:").append(crop).append(" ");
        if (part != null && !part.isBlank()) sb.append("部位:").append(part).append(" ");
        if (symptom != null && !symptom.isBlank()) sb.append("症状:").append(symptom).append(" ");
        if (environment != null && !environment.isBlank()) sb.append("环境:").append(environment).append(" ");
        return sb.toString().trim();
    }
}