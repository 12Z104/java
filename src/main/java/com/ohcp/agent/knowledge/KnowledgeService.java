package com.ohcp.agent.knowledge;

import com.ohcp.agent.entity.KnowledgeEntry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 知识库检索服务：加载 JSON 知识库，向量化后按“向量余弦 + 关键词 + 部位 + 作物”
 * 多路加权打分，返回带置信度的缺素候选。
 */
@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    private final KnowledgeLoader loader;
    private final Vectorizer vectorizer = new Vectorizer();

    @Value("${ohcp.knowledge.seed-location:classpath:knowledge.json}")
    private String seedLocation;

    @Value("${ohcp.knowledge.runtime-dir:data}")
    private String runtimeDir;

    private volatile List<KnowledgeEntry> entries = new ArrayList<>();
    private final Map<String, KnowledgeEntry> byId = new ConcurrentHashMap<>();
    private final Map<String, double[]> vectors = new ConcurrentHashMap<>();

    public KnowledgeService(KnowledgeLoader loader) {
        this.loader = loader;
    }

    @PostConstruct
    public void init() {
        try {
            Path runtime = Path.of(runtimeDir, "knowledge.json");
            List<KnowledgeEntry> loaded = loader.loadFromFile(runtime);
            if (loaded == null || loaded.isEmpty()) {
                loaded = loader.loadFromClasspath(seedLocation);
                log.info("已加载内置种子知识库，共 {} 条缺素知识", loaded.size());
            } else {
                log.info("已加载运行时知识库 {}，共 {} 条缺素知识", runtime.toAbsolutePath(), loaded.size());
            }
            rebuild(loaded);
        } catch (Exception e) {
            log.error("知识库初始化失败", e);
        }
    }

    public synchronized void rebuild(List<KnowledgeEntry> loaded) {
        if (loaded == null) {
            return;
        }
        entries = new ArrayList<>(loaded);
        byId.clear();
        vectors.clear();
        for (KnowledgeEntry entry : entries) {
            byId.put(entry.getId(), entry);
            vectors.put(entry.getId(), vectorizer.vectorize(entry.buildSearchText()));
        }
    }

    /** 多路加权检索，返回按置信度降序的候选 */
    public List<ScoredEntry> search(String query, String crop, String part) {
        if (entries.isEmpty() || query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        String normalizedQuery = normalize(query);
        double[] queryVector = vectorizer.vectorize(normalizedQuery);

        List<ScoredEntry> scored = new ArrayList<>();
        for (KnowledgeEntry entry : entries) {
            double[] entryVector = vectors.getOrDefault(entry.getId(), new double[Vectorizer.DIM]);
            double cosine = Vectorizer.cosine(queryVector, entryVector);
            double keyword = keywordScore(entry, normalizedQuery);
            double partScore = partScore(entry, part);
            double cropScore = cropScore(entry, crop);
            double raw = cosine * 0.45 + keyword * 0.35 + partScore * 0.10 + cropScore * 0.10;
            scored.add(new ScoredEntry(entry, raw, matchedSymptoms(entry, queryVector)));
        }
        scored.sort(Comparator.comparingDouble(ScoredEntry::getRawScore).reversed());

        if (!scored.isEmpty()) {
            double max = scored.get(0).getRawScore();
            for (ScoredEntry item : scored) {
                double confidence = max > 0 ? Math.min(0.99, item.getRawScore() / max) : 0;
                item.setConfidence(Math.round(confidence * 100.0) / 100.0);
            }
        }
        return scored;
    }

    private double keywordScore(KnowledgeEntry entry, String query) {
        List<String> keywords = entry.getKeywords();
        if (keywords == null || keywords.isEmpty()) {
            return 0;
        }
        int hit = 0;
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && query.contains(keyword)) {
                hit++;
            }
        }
        return Math.min(1.0, hit / Math.sqrt(keywords.size()));
    }

    private double partScore(KnowledgeEntry entry, String part) {
        if (part == null || part.isBlank()) {
            return 0.3;
        }
        for (String p : entry.getAffectedParts()) {
            if (p != null && !p.isBlank() && (part.contains(p) || p.contains(part))) {
                return 1.0;
            }
        }
        return 0.0;
    }

    private double cropScore(KnowledgeEntry entry, String crop) {
        boolean generic = entry.getCrops() == null || entry.getCrops().isEmpty() || entry.getCrops().contains("通用");
        if (crop == null || crop.isBlank()) {
            return 0.3;
        }
        for (String c : entry.getCrops()) {
            if (c != null && !c.isBlank() && !"通用".equals(c) && (crop.contains(c) || c.contains(crop))) {
                return 1.0;
            }
        }
        return generic ? 0.4 : 0.0;
    }

    private List<String> matchedSymptoms(KnowledgeEntry entry, double[] queryVector) {
        List<String> result = new ArrayList<>();
        if (entry.getSymptoms() == null || entry.getSymptoms().isEmpty()) {
            return result;
        }
        List<Map.Entry<String, Double>> symptomScores = new ArrayList<>();
        for (String symptom : entry.getSymptoms()) {
            symptomScores.add(Map.entry(symptom, Vectorizer.cosine(queryVector, vectorizer.vectorize(symptom))));
        }
        symptomScores.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        for (int i = 0; i < Math.min(2, symptomScores.size()); i++) {
            result.add(symptomScores.get(i).getKey());
        }
        return result;
    }

    private String normalize(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (Character.isLetterOrDigit(c) || Character.isIdeographic(c)) {
                sb.append(c);
            }
        }
        return sb.toString().toLowerCase();
    }

    public int size() {
        return entries.size();
    }

    public List<KnowledgeEntry> all() {
        return new ArrayList<>(entries);
    }

    public KnowledgeEntry get(String id) {
        return byId.get(id);
    }

    public static class ScoredEntry {
        private final KnowledgeEntry entry;
        private final double rawScore;
        private double confidence;
        private final List<String> matchedSymptoms;

        public ScoredEntry(KnowledgeEntry entry, double rawScore, List<String> matchedSymptoms) {
            this.entry = entry;
            this.rawScore = rawScore;
            this.matchedSymptoms = matchedSymptoms;
        }

        public KnowledgeEntry getEntry() { return entry; }
        public double getRawScore() { return rawScore; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public List<String> getMatchedSymptoms() { return matchedSymptoms; }
    }
}