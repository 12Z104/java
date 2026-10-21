package com.ohcp.agent.entity;

import java.util.ArrayList;
import java.util.List;

/** 结构化施肥建议 */
public class FertilizerAdvice {

    private String name;
    private String element;
    private String description;
    private List<String> agronomic = new ArrayList<>();
    private List<String> organicFertilizer = new ArrayList<>();
    private List<String> chemicalFertilizer = new ArrayList<>();
    private List<String> safety = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getElement() { return element; }
    public void setElement(String element) { this.element = element; }
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
}