package com.mangastudio.backend.service;

public interface TelemetryBufferService {
    void initializeSeries(Long seriesId);
    void recordChapterView(Long chapterId);
    void flushBufferToDatabase();
}
