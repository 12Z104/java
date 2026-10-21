package com.ohcp.agent.controller;

import com.ohcp.agent.crawler.CrawlerService;
import com.ohcp.agent.entity.CrawlerStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 爬虫管理接口：手动触发知识库增量更新 */
@RestController
@RequestMapping("/api/crawler")
public class CrawlerController {

    private final CrawlerService crawlerService;

    public CrawlerController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @PostMapping("/run")
    public CrawlerStatus run() {
        return crawlerService.run();
    }

    @GetMapping("/sources")
    public String sources() {
        return "爬虫在应用启动后自动运行一次；也可 POST /api/crawler/run 手动触发增量更新。";
    }
}