package com.ohcp.agent.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 基于 Jsoup 的网页抓取器，负责获取公开农技科普资料原文 */
@Component
public class WebFetcher {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

    public String fetch(String url, int timeoutSeconds) throws IOException {
        Document document = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(timeoutSeconds * 1000)
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .get();
        return document == null ? "" : document.text();
    }
}