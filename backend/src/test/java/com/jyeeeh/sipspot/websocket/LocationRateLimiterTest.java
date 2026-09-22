package com.jyeeeh.sipspot.websocket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocationRateLimiterTest {

    @Test
    void 첫_전송은_항상_허용된다() {
        LocationRateLimiter limiter = new LocationRateLimiter();
        assertThat(limiter.tryAcquire(1L)).isTrue();
    }

    @Test
    void 인터벌_내_재전송은_차단된다() {
        long accountId = 1L;
        LocationRateLimiter limiter = new LocationRateLimiter() {
            private long time = 1000L;
            @Override protected long currentTimeMs() { return time; }
        };

        assertThat(limiter.tryAcquire(accountId)).isTrue();
        assertThat(limiter.tryAcquire(accountId)).isFalse(); // 같은 ms → 차단
    }

    @Test
    void 인터벌_경과_후_재전송은_허용된다() {
        long accountId = 1L;
        long[] time = {1000L};
        LocationRateLimiter limiter = new LocationRateLimiter() {
            @Override protected long currentTimeMs() { return time[0]; }
        };

        assertThat(limiter.tryAcquire(accountId)).isTrue();

        time[0] += LocationRateLimiter.MIN_INTERVAL_MS; // 500ms 경과
        assertThat(limiter.tryAcquire(accountId)).isTrue();
    }

    @Test
    void 다른_계정은_서로_독립적으로_제한된다() {
        long[] time = {1000L};
        LocationRateLimiter limiter = new LocationRateLimiter() {
            @Override protected long currentTimeMs() { return time[0]; }
        };

        assertThat(limiter.tryAcquire(1L)).isTrue();
        assertThat(limiter.tryAcquire(2L)).isTrue(); // 다른 계정은 독립적
        assertThat(limiter.tryAcquire(1L)).isFalse(); // 1L만 차단
    }
}
