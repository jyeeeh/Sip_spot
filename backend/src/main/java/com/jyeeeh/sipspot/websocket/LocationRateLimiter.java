package com.jyeeeh.sipspot.websocket;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LocationRateLimiter {

    static final long MIN_INTERVAL_MS = 500L; // 초당 최대 2회

    private final ConcurrentHashMap<UUID, Long> lastSentMs = new ConcurrentHashMap<>();

    /**
     * 허용되면 true, 속도 초과이면 false (초과 시 무시 — 에러 없음).
     */
    public boolean tryAcquire(UUID memberId) {
        long now = currentTimeMs();
        long[] allowed = {0};
        lastSentMs.compute(memberId, (id, last) -> {
            if (last == null || now - last >= MIN_INTERVAL_MS) {
                allowed[0] = 1;
                return now;
            }
            return last;
        });
        return allowed[0] == 1;
    }

    // 테스트에서 오버라이드 가능하도록 분리
    protected long currentTimeMs() {
        return System.currentTimeMillis();
    }
}
