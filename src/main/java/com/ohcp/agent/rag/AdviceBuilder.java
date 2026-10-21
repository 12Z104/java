package com.ohcp.agent.rag;

import com.ohcp.agent.entity.DeficiencyCandidate;
import com.ohcp.agent.entity.FertilizerAdvice;
import com.ohcp.agent.entity.KnowledgeEntry;
import org.springframework.stereotype.Component;

import java.util.List;

/** 将知识库条目渲染为结构化施肥建议与诊断文本 */
@Component
public class AdviceBuilder {

    public FertilizerAdvice toAdvice(KnowledgeEntry entry) {
        FertilizerAdvice advice = new FertilizerAdvice();
        advice.setName(entry.getName());
        advice.setElement(entry.getElement());
        advice.setDescription(entry.getDescription());
        advice.setAgronomic(entry.getAgronomic());
        advice.setOrganicFertilizer(entry.getOrganicFertilizer());
        advice.setChemicalFertilizer(entry.getChemicalFertilizer());
        advice.setSafety(entry.getSafety());
        return advice;
    }

    public String renderDiagnosis(String crop, KnowledgeEntry top, List<DeficiencyCandidate> candidates) {
        StringBuilder sb = new StringBuilder();
        String cropText = (crop == null || crop.isBlank()) ? "未明确作物" : crop;
        sb.append("【缺素诊断】\n");
        sb.append("作物：").append(cropText).append("\n");
        if (!candidates.isEmpty()) {
            DeficiencyCandidate first = candidates.get(0);
            sb.append("识别结果：").append(first.getName())
              .append("（元素 ").append(first.getElement()).append("）")
              .append("，匹配置信度 ").append(percent(first.getConfidence())).append("\n");
        }
        if (candidates.size() > 1) {
            StringBuilder others = new StringBuilder("候选：");
            for (int i = 1; i < candidates.size(); i++) {
                DeficiencyCandidate c = candidates.get(i);
                if (i > 1) others.append("、");
                others.append(c.getName()).append(" ").append(percent(c.getConfidence()));
            }
            sb.append(others).append("\n");
        }
        if (!top.getSymptoms().isEmpty()) {
            sb.append("典型症状：").append(String.join("；", top.getSymptoms().subList(0, Math.min(3, top.getSymptoms().size())))).append("\n");
        }
        sb.append("\n【缺素简介】\n").append(top.getDescription()).append("\n");
        sb.append("\n【农艺调控手段】\n").append(listText(top.getAgronomic()));
        sb.append("\n【有机/生物肥施用方案】\n").append(listText(top.getOrganicFertilizer()));
        sb.append("\n【化学施肥方案】\n").append(listText(top.getChemicalFertilizer()));
        sb.append("\n【施肥安全与减量提醒】\n").append(listText(top.getSafety()));
        return sb.toString().trim();
    }

    private String listText(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "请结合当地农技部门测土配方施肥意见实施。";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            sb.append(i + 1).append(". ").append(items.get(i)).append("\n");
        }
        return sb.toString().trim();
    }

    private String percent(double confidence) {
        return String.format("%.0f%%", confidence * 100);
    }
}