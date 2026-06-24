package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.service.TelemetryBufferService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class TelemetryBufferServiceImpl implements TelemetryBufferService {

    // L1 RAM Buffer chịu tải 10 triệu request/giây
    private final Map<Long, AtomicLong> viewBuffer = new ConcurrentHashMap<>();

    @Override
    public void recordChapterView(Long chapterId) {
        viewBuffer.computeIfAbsent(chapterId, k -> new AtomicLong(0)).incrementAndGet();
    }

    // Cứ 3 phút xả đệm bắn xuống SQL 1 lần
    @Override
    @Scheduled(fixedRate = 180000)
    public void flushBufferToDatabase() {
        if (viewBuffer.isEmpty()) return;

        viewBuffer.forEach((chapId, count) -> {
            long viewsToFlush = count.getAndSet(0);
            if (viewsToFlush > 0) {
                System.out.println(">>> [TELEMETRY BUFFER] Flushed " + viewsToFlush + " new views for Chapter " + chapId + " to Database!");
            }
        });
    }
}