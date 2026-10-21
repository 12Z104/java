package com.ohcp.agent.entity;

import java.util.ArrayList;
import java.util.List;

/** 知识库条目：一条缺素症知识（由爬虫清洗、结构化后落盘） */
public class KnowledgeEntry {

    private String id;
    private List<String> crops = new ArrayList<>();
    private String element;
    private String name;
    private List<String> aliases = new ArrayList<>();
    private List<String> affectedParts = new ArrayList<>();
    private List<String> symptoms = new ArrayList<>();
    private List<String> keywords = new ArrayList<>();
    private String description;
    private List<String> agronomic = new ArrayList<>();
    private List<String> organicFertilizer = new ArrayList<>();
    private List<String> chemicalFertilizer = new ArrayList<>();
    private List<String> safety = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public List<String> getCrops() { return crops; }
    public void setCrops(List<String> crops) { this.crops = crops; }
    public String getElement() { return element; }
    public void setElement(String element) { this.element = element; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getAliases() { return aliases; }
    public void setAliases(List<String> aliases) { this.aliases = aliases; }
    public List<String> getAffectedParts() { return affectedParts; }
    public void setAffectedParts(List<String> affectedParts) { this.affectedParts = affectedParts; }
    public List<String> getSymptoms() { return symptoms; }
    public void setSymptoms(List<String> symptoms) { this.symptoms = symptoms; }
    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getAgronomic() { return agronomic; }
    public void setAgronomic(List<String> agronomic) { this.agronomic = agronomic; }
    public List<String> getOrganicFertilizer() { return organicFertilizer; }
    public void setOrganicFertilizer(List<String> organicFertilizer) { this.organicFertilizer = organicFertilizer; }
    public List<String> getChemicalFertilizer() { return chemicalFertilizer; }
    public void setChemicalFertilizer(List<String> chemicalFertilizer) { this.chemicalFertilizer = chemicalFertilizer; }
    public List<String> getSafety() { return safety; }
    public void setSafety(List<String> safety) { this.safety = safety; }

    /** 组合检索文本：作物 + 部位 + 症状 + 关键词 */
    public String buildSearchText() {
        StringBuilder sb = new StringBuilder();
        for (String c : crops) sb.append(c).append(" ");
        for (String p : affectedParts) sb.append(p).append(" ");
        for (String s : symptoms) sb.append(s).append(" ");
        for (String k : keywords) sb.append(k).append(" ");
        if (aliases != null) for (String a : aliases) sb.append(a).append(" ");
        return sb.toString().trim();
    }
}