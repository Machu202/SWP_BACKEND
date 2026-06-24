package com.mangastudio.backend.service;

public interface TelemetryBufferService {
    void recordChapterView(Long chapterId);
    void flushBufferToDatabase();
}