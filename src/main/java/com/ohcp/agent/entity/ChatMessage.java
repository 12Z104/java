package com.ohcp.agent.entity;

/** 会话消息：用于多轮对话上下文记忆 */
public class ChatMessage {

    private String role;
    private String content;
    private long timestamp;

    public ChatMessage() {}
    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}