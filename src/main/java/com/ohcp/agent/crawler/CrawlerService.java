package com.ohcp.agent.crawler;

import com.ohcp.agent.entity.CrawlerStatus;
import com.ohcp.agent.entity.KnowledgeEntry;
import com.ohcp.agent.knowledge.KnowledgeLoader;
import com.ohcp.agent.knowledge.KnowledgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 爬虫服务：抓取公开农技科普资料 -> 清洗、分句 -> 按元素/症状关键词结构化 ->
 * 合并去重后写入本地 JSON 知识库（增量更新，不改代码即可扩充知识）。
 */
@Service
public class CrawlerService {

    private static final Logger log = LoggerFactory.getLogger(CrawlerService.class);

    private final WebFetcher fetcher;
    private final KnowledgeService knowledgeService;
    private final KnowledgeLoader loader;

    @Value("${ohcp.crawler.timeout-seconds:12}")
    private int timeoutSeconds;

    @Value("${ohcp.crawler.auto-run:true}")
    private boolean autoRun;

    @Value("${ohcp.knowledge.runtime-dir:data}")
    private String runtimeDir;

    /** 公开农技科普资料地址，可按需替换为稳定可访问的页面 */
    private static final List<String> SEED_URLS = List.of(
            "https://www.moa.gov.cn/ztzl/nykp/",
            "https://www.gov.cn/zhengce/zuixin/"
    );

    private static final Map<String, List<String>> ELEMENT_KEYWORDS = new LinkedHashMap<>();
    static {
        ELEMENT_KEYWORDS.put("氮", List.of("缺氮", "氮素缺乏", "氮肥不足"));
        ELEMENT_KEYWORDS.put("磷", List.of("缺磷", "磷素缺乏", "磷肥不足"));
        ELEMENT_KEYWORDS.put("钾", List.of("缺钾", "钾素缺乏", "钾肥不足"));
        ELEMENT_KEYWORDS.put("铁", List.of("缺铁", "铁素缺乏"));
        ELEMENT_KEYWORDS.put("锌", List.of("缺锌", "锌素缺乏"));
        ELEMENT_KEYWORDS.put("镁", List.of("缺镁", "镁素缺乏"));
        ELEMENT_KEYWORDS.put("钙", List.of("缺钙", "钙素缺乏"));
        ELEMENT_KEYWORDS.put("硼", List.of("缺硼", "硼素缺乏"));
        ELEMENT_KEYWORDS.put("钼", List.of("缺钼", "钼素缺乏"));
        ELEMENT_KEYWORDS.put("锰", List.of("缺锰", "锰素缺乏"));
        ELEMENT_KEYWORDS.put("硫", List.of("缺硫", "硫素缺乏"));
        ELEMENT_KEYWORDS.put("铜", List.of("缺铜", "铜素缺乏"));
    }

    private static final List<String> SYMPTOM_KEYWORDS = List.of(
            "黄", "紫", "红", "斑", "焦", "枯", "矮", "小叶", "失绿", "白化", "坏死",
            "卷", "畸形", "簇生", "发黄", "叶脉间", "叶缘", "分蘖", "根", "花", "果");

    private static final List<String> PART_KEYWORDS = List.of("老叶", "新叶", "幼叶", "叶脉间", "叶缘", "叶尖", "顶芽", "茎", "根", "果实", "整株");

    public CrawlerService(WebFetcher fetcher, KnowledgeService knowledgeService, KnowledgeLoader loader) {
        this.fetcher = fetcher;
        this.knowledgeService = knowledgeService;
        this.loader = loader;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void autoRunOnStartup() {
        if (!autoRun) {
            return;
        }
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                run();
            } catch (Exception e) {
                log.warn("启动爬虫执行异常: {}", e.getMessage());
            }
        }, "ohcp-crawler").start();
    }

    public CrawlerStatus run() {
        long start = System.currentTimeMillis();
        CrawlerStatus status = new CrawlerStatus();
        List<KnowledgeEntry> newEntries = new ArrayList<>();
        int fetched = 0;

        for (String url : SEED_URLS) {
            try {
                String text = fetcher.fetch(url, timeoutSeconds);
                if (text == null || text.isBlank()) {
                    status.getSources().add(url + "（空内容）");
                    continue;
                }
                fetched++;
                status.getSources().add(url);
                List<KnowledgeEntry> parsed = parseAndStructure(text);
                newEntries.addAll(parsed);
                status.setExtractedEntries(status.getExtractedEntries() + parsed.size());
            } catch (Exception e) {
                log.warn("爬取失败 {}: {}", url, e.getMessage());
                status.getSources().add(url + "（失败）");
            }
        }
        status.setFetchedPages(fetched);

        if (newEntries.isEmpty()) {
            status.setSuccess(false);
            status.setMessage("未抓取到可结构化新知识（网络不可用或无匹配内容），继续使用内置种子知识库，共 "
                    + knowledgeService.size() + " 条。");
        } else {
            List<KnowledgeEntry> merged = merge(knowledgeService.all(), newEntries);
            try {
                loader.writeToFile(Path.of(runtimeDir, "knowledge.json"), merged);
                knowledgeService.rebuild(merged);
                status.setSuccess(true);
                status.setMessage("爬虫完成：新增 " + newEntries.size() + " 条，知识库共 " + merged.size() + " 条，已增量写入本地 JSON。");
            } catch (Exception e) {
                status.setSuccess(false);
                status.setMessage("知识库写入失败: " + e.getMessage());
            }
        }
        status.setTotalEntries(knowledgeService.size());
        status.setElapsedMs(System.currentTimeMillis() - start);
        return status;
    }

    /** 清洗、分句并按元素+症状关键词结构化 */
    List<KnowledgeEntry> parseAndStructure(String text) {
        List<KnowledgeEntry> entries = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return entries;
        }
        String cleaned = text.replaceAll("\\s+", " ").replaceAll("\\u3000", "");
        for (String rawSentence : cleaned.split("[。；！？\\n]")) {
            String sentence = rawSentence.trim();
            if (sentence.length() < 8 || sentence.length() > 120) {
                continue;
            }
            String element = detectElement(sentence);
            if (element == null) {
                continue;
            }
            long symptomHits = SYMPTOM_KEYWORDS.stream().filter(sentence::contains).count();
            if (symptomHits < 2) {
                continue;
            }
            KnowledgeEntry entry = new KnowledgeEntry();
            entry.setId("crawl-" + Integer.toHexString(sentence.hashCode()));
            entry.setCrops(new ArrayList<>(List.of("通用")));
            entry.setElement(element);
            entry.setName("缺" + element);
            entry.setSymptoms(new ArrayList<>(List.of(sentence)));
            entry.setDescription(sentence);
            entry.setAffectedParts(detectParts(sentence));
            entry.setKeywords(buildKeywords(element, sentence));
            entry.setAgronomic(defaultAdvice("农艺", element));
            entry.setOrganicFertilizer(defaultAdvice("有机肥", element));
            entry.setChemicalFertilizer(defaultAdvice("化学肥", element));
            entry.setSafety(defaultAdvice("安全", element));
            entries.add(entry);
        }
        return entries;
    }

    private String detectElement(String sentence) {
        for (Map.Entry<String, List<String>> e : ELEMENT_KEYWORDS.entrySet()) {
            for (String keyword : e.getValue()) {
                if (sentence.contains(keyword)) {
                    return e.getKey();
                }
            }
        }
        return null;
    }

    private List<String> detectParts(String sentence) {
        List<String> parts = new ArrayList<>();
        for (String p : PART_KEYWORDS) {
            if (sentence.contains(p)) {
                parts.add(p);
            }
        }
        return parts.isEmpty() ? new ArrayList<>(List.of("整株")) : parts;
    }

    private List<String> buildKeywords(String element, String sentence) {
        List<String> keywords = new ArrayList<>();
        keywords.add(element);
        for (String s : SYMPTOM_KEYWORDS) {
            if (sentence.contains(s)) {
                keywords.add(s);
            }
        }
        return keywords;
    }

    private List<String> defaultAdvice(String type, String element) {
        return switch (type) {
            case "农艺" -> new ArrayList<>(List.of(
                    "增施充分腐熟的有机肥，改善土壤结构，提高养分有效性。",
                    "结合当地农技部门开展测土配方施肥，平衡氮磷钾与中微量元素投入。"));
            case "有机肥" -> new ArrayList<>(List.of(
                    "基施充分腐熟的有机肥或商品有机肥，配合生物菌肥改良根际环境。",
                    "建议在农技人员指导下选择含" + element + "的有机/生物投入品。"));
            case "化学肥" -> new ArrayList<>(List.of(
                    "依据土壤测试与作物需肥规律，选择含" + element + "的化学肥料分次追施。",
                    "叶面喷施含" + element + "的微肥溶液作为快速补充，注意浓度与时期。"));
            default -> new ArrayList<>(List.of(
                    "坚持化肥减量增效，少量多次，避免过量施肥造成土壤板结与面源污染。",
                    "具体用量请结合土壤检测与当地农技推广部门意见确定。"));
        };
    }

    private List<KnowledgeEntry> merge(List<KnowledgeEntry> existing, List<KnowledgeEntry> incoming) {
        Set<String> seen = new HashSet<>();
        List<KnowledgeEntry> merged = new ArrayList<>();
        for (KnowledgeEntry entry : existing) {
            if (seen.add(normKey(entry))) {
                merged.add(entry);
            }
        }
        for (KnowledgeEntry entry : incoming) {
            if (seen.add(normKey(entry))) {
                merged.add(entry);
            }
        }
        return merged;
    }

    private String normKey(KnowledgeEntry entry) {
        String text = entry.getId() + "|" + String.join(",", entry.getSymptoms());
        return Integer.toHexString(text.hashCode());
    }
}