package com.jyeeeh.sipspot.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimiter {

    // 입장 실패(404)만 카운트: IP당 15분 10회
    private static final int JOIN_FAIL_LIMIT = 10;
    private static final long JOIN_WINDOW_MS = 15 * 60 * 1000L;

    // 방 생성: IP당 1시간 20회
    private static final int CREATE_LIMIT = 20;
    private static final long CREATE_WINDOW_MS = 60 * 60 * 1000L;

    private final ConcurrentHashMap<String, Window> joinFailMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Window> createMap = new ConcurrentHashMap<>();

    public boolean isJoinFailBlocked(String ip) {
        return isBlocked(joinFailMap, ip, JOIN_WINDOW_MS, JOIN_FAIL_LIMIT);
    }

    public void recordJoinFail(String ip) {
        record(joinFailMap, ip, JOIN_WINDOW_MS);
    }

    public boolean isCreateBlocked(String ip) {
        return isBlocked(createMap, ip, CREATE_WINDOW_MS, CREATE_LIMIT);
    }

    public void recordCreate(String ip) {
        record(createMap, ip, CREATE_WINDOW_MS);
    }

    private boolean isBlocked(ConcurrentHashMap<String, Window> map, String ip,
                               long windowMs, int limit) {
        Window w = map.get(ip);
        if (w == null) return false;
        if (Instant.now().toEpochMilli() - w.startMs > windowMs) return false;
        return w.count.get() >= limit;
    }

    private void record(ConcurrentHashMap<String, Window> map, String ip, long windowMs) {
        long now = Instant.now().toEpochMilli();
        map.compute(ip, (k, w) -> {
            if (w == null || now - w.startMs > windowMs) {
                Window fresh = new Window(now);
                fresh.count.set(1);
                return fresh;
            }
            w.count.incrementAndGet();
            return w;
        });
    }

    // 만료된 항목을 5분마다 정리
    @Scheduled(fixedDelay = 5 * 60 * 1000L)
    public void evictExpired() {
        long now = Instant.now().toEpochMilli();
        joinFailMap.entrySet().removeIf(e -> now - e.getValue().startMs > JOIN_WINDOW_MS);
        createMap.entrySet().removeIf(e -> now - e.getValue().startMs > CREATE_WINDOW_MS);
    }

    private static class Window {
        final long startMs;
        final AtomicInteger count = new AtomicInteger(0);

        Window(long startMs) {
            this.startMs = startMs;
        }
    }
}
