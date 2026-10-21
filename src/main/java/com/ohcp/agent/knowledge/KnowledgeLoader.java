package com.ohcp.agent.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ohcp.agent.entity.KnowledgeEntry;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** 知识库加载器：支持 classpath 种子知识库与运行时 JSON 文件两种来源 */
@Component
public class KnowledgeLoader {

    private final ObjectMapper objectMapper;

    public KnowledgeLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<KnowledgeEntry> loadFromClasspath(String location) {
        try {
            Resource resource = new ClassPathResource(location.startsWith("classpath:") ? location.substring("classpath:".length()) : location);
            try (InputStream in = resource.getInputStream()) {
                return objectMapper.readValue(in, new TypeReference<List<KnowledgeEntry>>() {});
            }
        } catch (Exception e) {
            throw new IllegalStateException("无法加载种子知识库: " + location, e);
        }
    }

    public List<KnowledgeEntry> loadFromFile(Path file) {
        try {
            if (!Files.exists(file)) {
                return null;
            }
            String json = Files.readString(file, StandardCharsets.UTF_8);
            return objectMapper.readValue(json, new TypeReference<List<KnowledgeEntry>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("无法加载运行时知识库: " + file, e);
        }
    }

    public void writeToFile(Path file, List<KnowledgeEntry> entries) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(entries);
            Files.writeString(file, json, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("无法写入知识库文件: " + file, e);
        }
    }
}