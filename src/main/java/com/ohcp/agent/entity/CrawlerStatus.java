package com.ohcp.agent.entity;

import java.util.ArrayList;
import java.util.List;

/** 爬虫执行状态 */
public class CrawlerStatus {

    private boolean success;
    private String message;
    private int fetchedPages;
    private int extractedEntries;
    private int totalEntries;
    private long elapsedMs;
    private List<String> sources = new ArrayList<>();

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public int getFetchedPages() { return fetchedPages; }
    public void setFetchedPages(int fetchedPages) { this.fetchedPages = fetchedPages; }
    public int getExtractedEntries() { return extractedEntries; }
    public void setExtractedEntries(int extractedEntries) { this.extractedEntries = extractedEntries; }
    public int getTotalEntries() { return totalEntries; }
    public void setTotalEntries(int totalEntries) { this.totalEntries = totalEntries; }
    public long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(long elapsedMs) { this.elapsedMs = elapsedMs; }
    public List<String> getSources() { return sources; }
    public void setSources(List<String> sources) { this.sources = sources; }
}