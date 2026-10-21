package com.ohcp.agent.rag;

import com.ohcp.agent.entity.ChatMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 多轮对话记忆：按会话保存最近上下文，支持追问缺素细节、用量、不同作物适配 */
@Service
public class MemoryService {

    private static final int MAX_TURNS = 12;

    private final Map<String, Deque<ChatMessage>> sessions = new ConcurrentHashMap<>();

    public String ensureSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        sessions.computeIfAbsent(sessionId, k -> new ArrayDeque<>());
        return sessionId;
    }

    public void add(String sessionId, String role, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        Deque<ChatMessage> deque = sessions.computeIfAbsent(sessionId, k -> new ArrayDeque<>());
        deque.addLast(new ChatMessage(role, content));
        while (deque.size() > MAX_TURNS) {
            deque.removeFirst();
        }
    }

    public List<ChatMessage> history(String sessionId) {
        return new ArrayList<>(sessions.computeIfAbsent(sessionId, k -> new ArrayDeque<>()));
    }

    /** 取最近若干轮对话拼接为上下文，供 RAG 检索增强使用 */
    public String recentContext(String sessionId, int maxTurns) {
        List<ChatMessage> all = history(sessionId);
        int from = Math.max(0, all.size() - maxTurns * 2);
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < all.size(); i++) {
            sb.append(all.get(i).getRole()).append(": ").append(all.get(i).getContent()).append(" ");
        }
        return sb.toString().trim();
    }

    public void clear(String sessionId) {
        sessions.remove(sessionId);
    }
}