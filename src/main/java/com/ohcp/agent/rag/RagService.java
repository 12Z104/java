package com.ohcp.agent.rag;

import com.ohcp.agent.entity.ChatRequest;
import com.ohcp.agent.entity.ChatResponse;
import com.ohcp.agent.entity.DeficiencyCandidate;
import com.ohcp.agent.entity.FertilizerAdvice;
import com.ohcp.agent.entity.KnowledgeEntry;
import com.ohcp.agent.knowledge.KnowledgeService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

/** RAG 智能体编排：实体抽取 -> 多轮上下文 -> 知识库检索 -> 建议生成 -> 思政融入 -> 大模型润色 */
@Service
public class RagService {

    private final KnowledgeService knowledgeService;
    private final EntityExtractor entityExtractor;
    private final MemoryService memoryService;
    private final AdviceBuilder adviceBuilder;
    private final IdeologyProvider ideologyProvider;
    private final LlmService llmService;

    public RagService(KnowledgeService knowledgeService,
                      EntityExtractor entityExtractor,
                      MemoryService memoryService,
                      AdviceBuilder adviceBuilder,
                      IdeologyProvider ideologyProvider,
                      LlmService llmService) {
        this.knowledgeService = knowledgeService;
        this.entityExtractor = entityExtractor;
        this.memoryService = memoryService;
        this.adviceBuilder = adviceBuilder;
        this.ideologyProvider = ideologyProvider;
        this.llmService = llmService;
    }

    public ChatResponse handle(ChatRequest request) {
        long start = System.currentTimeMillis();
        String sessionId = memoryService.ensureSession(request.getSessionId());
        String query = request.buildQueryText();
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);

        if (query.isBlank()) {
            response.setAnswer("请输入作物缺素症状描述，例如：水稻，叶片从下部老叶开始均匀发黄，植株矮小，分蘖减少。");
            response.setElapsedMs(System.currentTimeMillis() - start);
            return response;
        }

        memoryService.add(sessionId, "user", query);

        EntityExtractor.ExtractedEntities entities = entityExtractor.extract(query);
        String crop = firstNonBlank(request.getCrop(), entities.getCrop());
        String part = firstNonBlank(request.getPart(), entities.getPart());

        // 多轮对话上下文参与检索，支持追问缺素细节、用量、不同作物适配
        String context = memoryService.recentContext(sessionId, 6);
        String searchQuery = context.isBlank() ? query : (context + " " + query);

        List<KnowledgeService.ScoredEntry> scored = knowledgeService.search(searchQuery, crop, part);
        response.setKnowledgeHits(scored.size());

        if (scored.isEmpty()) {
            String fallback = "抱歉，本地知识库暂未检索到高置信度的缺素类型。建议补充：作物类型、症状部位（老叶/新叶）、症状表现（黄化/焦枯/斑点/矮小等），我将为您重新诊断。";
            response.setAnswer(fallback);
            memoryService.add(sessionId, "assistant", fallback);
            response.setElapsedMs(System.currentTimeMillis() - start);
            return response;
        }

        List<DeficiencyCandidate> candidates = new ArrayList<>();
        Set<String> seenElements = new HashSet<>();
        for (KnowledgeService.ScoredEntry item : scored) {
            if (candidates.size() >= 4) {
                break;
            }
            if (!seenElements.add(item.getEntry().getElement())) {
                continue;
            }
            DeficiencyCandidate candidate = new DeficiencyCandidate();
            candidate.setName(item.getEntry().getName());
            candidate.setElement(item.getEntry().getElement());
            candidate.setConfidence(item.getConfidence());
            candidate.setMatchedSymptoms(item.getMatchedSymptoms());
            candidates.add(candidate);
        }
        response.setCandidates(candidates);

        KnowledgeEntry top = scored.get(0).getEntry();
        FertilizerAdvice advice = adviceBuilder.toAdvice(top);
        response.setAdvice(advice);

        response.setSources(List.of(
                "本地知识库条目 #" + top.getId(),
                "知识库总量 " + knowledgeService.size() + " 条"
        ));

        String diagnosis = adviceBuilder.renderDiagnosis(crop, top, candidates);
        String polished = llmService.polish(buildPolishPrompt(diagnosis));
        String answer;
        if (polished != null && !polished.isBlank()) {
            answer = diagnosis + "\n\n【AI 深度解读】\n" + polished.trim();
            response.setLlmEnhanced(true);
        } else {
            answer = diagnosis;
            response.setLlmEnhanced(false);
        }
        answer = answer + "\n\n🌾 " + ideologyProvider.provide(crop);
        response.setAnswer(answer);

        memoryService.add(sessionId, "assistant", answer);
        response.setElapsedMs(System.currentTimeMillis() - start);
        return response;
    }

    private String buildPolishPrompt(String diagnosis) {
        return "你是农业施肥专家。请根据以下从本地知识库检索到的内容，用不超过120字整理一段通俗易懂的诊断与施肥要点总结，"
                + "必须忠实于知识库内容，不得编造：\n" + diagnosis;
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        return (b == null || b.isBlank()) ? "" : b.trim();
    }
}