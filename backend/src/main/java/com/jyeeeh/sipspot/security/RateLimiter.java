package com.jyeeeh.sipspot.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimiter {

    // 로그인 실패: IP당 15분 10회
    private static final int LOGIN_LIMIT = 10;
    private static final long LOGIN_WINDOW_MS = 15 * 60 * 1000L;

    // 회원가입: IP당 1시간 5회
    private static final int SIGNUP_LIMIT = 5;
    private static final long SIGNUP_WINDOW_MS = 60 * 60 * 1000L;

    private final ConcurrentHashMap<String, Window> loginMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Window> signupMap = new ConcurrentHashMap<>();

    public boolean isLoginBlocked(String ip) {
        return isBlocked(loginMap, ip, LOGIN_WINDOW_MS, LOGIN_LIMIT);
    }

    public void recordLogin(String ip) {
        record(loginMap, ip, LOGIN_WINDOW_MS);
    }

    public boolean isSignupBlocked(String ip) {
        return isBlocked(signupMap, ip, SIGNUP_WINDOW_MS, SIGNUP_LIMIT);
    }

    public void recordSignup(String ip) {
        record(signupMap, ip, SIGNUP_WINDOW_MS);
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
        loginMap.entrySet().removeIf(e -> now - e.getValue().startMs > LOGIN_WINDOW_MS);
        signupMap.entrySet().removeIf(e -> now - e.getValue().startMs > SIGNUP_WINDOW_MS);
    }

    private static class Window {
        final long startMs;
        final AtomicInteger count = new AtomicInteger(0);

        Window(long startMs) {
            this.startMs = startMs;
        }
    }
}
