package com.ohcp.agent.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** 从用户自然语言中抽取作物、症状部位、环境等实体 */
@Component
public class EntityExtractor {

    private static final List<String> CROPS = Arrays.asList(
            "西红柿", "马铃薯", "水稻", "小麦", "玉米", "大豆", "花生", "油菜", "棉花",
            "黄瓜", "番茄", "辣椒", "茄子", "白菜", "苹果", "柑橘", "葡萄", "香蕉",
            "桃树", "梨树", "草莓", "西瓜", "甜瓜", "茶树", "甘蔗", "高粱", "谷子", "燕麦", "蔬菜", "果树");

    private static final List<String> PARTS = Arrays.asList(
            "生长点", "中下部叶", "下部叶片", "上部叶片", "下部老叶", "叶脉间", "叶脉", "叶缘", "叶尖",
            "老叶", "新叶", "幼叶", "嫩叶", "顶芽", "顶梢", "茎秆", "根系", "果实", "花", "整株", "全株", "幼苗", "苗期");

    private static final List<String> ENVIRONMENTS = Arrays.asList(
            "石灰性土壤", "土壤板结", "过量施氮", "磷过量", "氮肥过量", "沙土", "砂土", "黏土", "粘土",
            "酸性土壤", "碱性土壤", "盐碱", "干旱", "积水", "涝", "低温", "高温", "寡照", "大棚", "露地");

    public ExtractedEntities extract(String query) {
        ExtractedEntities result = new ExtractedEntities();
        if (query == null || query.isBlank()) {
            return result;
        }
        result.setRaw(query.trim());
        result.setCrop(matchLongest(query, CROPS));
        result.setPart(matchLongest(query, PARTS));
        List<String> envHits = new ArrayList<>();
        for (String env : ENVIRONMENTS) {
            if (query.contains(env)) {
                envHits.add(env);
            }
        }
        result.setEnvironment(envHits.isEmpty() ? "" : String.join("、", envHits));
        return result;
    }

    private String matchLongest(String query, List<String> dict) {
        return dict.stream()
                .filter(query::contains)
                .max(Comparator.comparingInt(String::length))
                .orElse("");
    }

    public static class ExtractedEntities {
        private String raw;
        private String crop;
        private String part;
        private String environment;

        public String getRaw() { return raw; }
        public void setRaw(String raw) { this.raw = raw; }
        public String getCrop() { return crop; }
        public void setCrop(String crop) { this.crop = crop; }
        public String getPart() { return part; }
        public void setPart(String part) { this.part = part; }
        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
    }
}