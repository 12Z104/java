package com.ohcp.agent.entity;

import java.util.ArrayList;
import java.util.List;

/** 缺素候选：名称、元素与匹配置信度 */
public class DeficiencyCandidate {

    private String name;
    private String element;
    private double confidence;
    private List<String> matchedSymptoms = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getElement() { return element; }
    public void setElement(String element) { this.element = element; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public List<String> getMatchedSymptoms() { return matchedSymptoms; }
    public void setMatchedSymptoms(List<String> matchedSymptoms) { this.matchedSymptoms = matchedSymptoms; }
}