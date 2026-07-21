package com.mangastudio.backend.component;

import com.mangastudio.backend.service.RuntimeSystemParameterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/** Applies database-backed scheduler intervals without requiring an application restart. */
@Component
@RequiredArgsConstructor
public class ConfigurableSchedulerGate {

    private final RuntimeSystemParameterService runtimeParameters;
    private final ConcurrentMap<String, AtomicLong> lastRuns = new ConcurrentHashMap<>();

    public boolean shouldRun(String jobId, String intervalParameter, int fallbackSeconds) {
        int intervalSeconds = runtimeParameters.positiveInteger(intervalParameter, fallbackSeconds, 86_400);
        long now = System.nanoTime();
        long requiredNanos = intervalSeconds * 1_000_000_000L;
        AtomicLong lastRun = lastRuns.computeIfAbsent(jobId, ignored -> new AtomicLong(0));
        while (true) {
            long previous = lastRun.get();
            if (previous != 0 && now - previous < requiredNanos) return false;
            if (lastRun.compareAndSet(previous, now)) return true;
        }
    }
}
