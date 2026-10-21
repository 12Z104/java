package com.ohcp.agent.controller;

import com.ohcp.agent.entity.ChatMessage;
import com.ohcp.agent.entity.ChatRequest;
import com.ohcp.agent.entity.ChatResponse;
import com.ohcp.agent.entity.KnowledgeEntry;
import com.ohcp.agent.knowledge.KnowledgeService;
import com.ohcp.agent.rag.MemoryService;
import com.ohcp.agent.rag.RagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 智能体核心接口：聊天诊断、会话历史、知识库统计、健康检查 */
@RestController
@RequestMapping("/api")
public class AgentController {

    private final RagService ragService;
    private final KnowledgeService knowledgeService;
    private final MemoryService memoryService;

    public AgentController(RagService ragService, KnowledgeService knowledgeService, MemoryService memoryService) {
        this.ragService = ragService;
        this.knowledgeService = knowledgeService;
        this.memoryService = memoryService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return ragService.handle(request);
    }

    @GetMapping("/session/{sessionId}/history")
    public List<ChatMessage> history(@PathVariable String sessionId) {
        return memoryService.history(sessionId);
    }

    @DeleteMapping("/session/{sessionId}")
    public Map<String, Object> clear(@PathVariable String sessionId) {
        memoryService.clear(sessionId);
        return Map.of("ok", true, "sessionId", sessionId);
    }

    @GetMapping("/knowledge/stats")
    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", knowledgeService.size());
        List<Map<String, String>> preview = knowledgeService.all().stream()
                .limit(20)
                .map(entry -> {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("id", entry.getId());
                    m.put("name", entry.getName());
                    m.put("element", entry.getElement());
                    return m;
                })
                .toList();
        result.put("entries", preview);
        return result;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "knowledgeEntries", knowledgeService.size());
    }
}