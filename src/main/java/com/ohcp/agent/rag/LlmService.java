package com.ohcp.agent.rag;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 可选大模型服务：优先 LangChain4j，其次 SpringAI；未配置 API Key 时返回 null，
 * 由上层自动回退为知识库模板输出，保证离线可演示。
 */
@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    @Value("${spring.ai.openai.api-key:}")
    private String springAiKey;
    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String springAiBaseUrl;
    @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}")
    private String springAiModel;

    @Value("${langchain4j.open-ai.chat-model.api-key:}")
    private String langchain4jKey;
    @Value("${langchain4j.open-ai.chat-model.base-url:https://api.openai.com/v1}")
    private String langchain4jBaseUrl;
    @Value("${langchain4j.open-ai.chat-model.model-name:gpt-4o-mini}")
    private String langchain4jModel;

    private volatile ChatModel springAiModelRef;
    private volatile ChatLanguageModel langchain4jModelRef;
    private volatile boolean initialized = false;
    private volatile boolean ready = false;

    public synchronized boolean available() {
        init();
        return ready;
    }

    private void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            if (hasText(langchain4jKey)) {
                langchain4jModelRef = dev.langchain4j.model.openai.OpenAiChatModel.builder()
                        .apiKey(langchain4jKey)
                        .baseUrl(langchain4jBaseUrl)
                        .modelName(langchain4jModel)
                        .build();
                ready = true;
                log.info("LangChain4j 大模型已启用: {}", langchain4jModel);
                return;
            }
            if (hasText(springAiKey)) {
                OpenAiApi openAiApi = OpenAiApi.builder()
                        .apiKey(springAiKey)
                        .baseUrl(springAiBaseUrl)
                        .build();
                springAiModelRef = OpenAiChatModel.builder()
                        .openAiApi(openAiApi)
                        .defaultOptions(OpenAiChatOptions.builder().model(springAiModel).build())
                        .build();
                ready = true;
                log.info("SpringAI 大模型已启用: {}", springAiModel);
                return;
            }
            log.info("未配置大模型 API Key，采用本地知识库模板输出（离线可用）");
        } catch (Exception e) {
            ready = false;
            log.warn("大模型初始化失败，回退本地知识库模板输出: {}", e.getMessage());
        }
    }

    /** 返回大模型整理结果；不可用或失败时返回 null */
    public String polish(String prompt) {
        if (!available()) {
            return null;
        }
        try {
            if (langchain4jModelRef != null) {
                return langchain4jModelRef.generate(prompt);
            }
            if (springAiModelRef != null) {
                return springAiModelRef.call(prompt);
            }
        } catch (Exception e) {
            log.warn("大模型调用失败，回退本地知识库模板输出: {}", e.getMessage());
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}