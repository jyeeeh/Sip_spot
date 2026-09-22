package com.jyeeeh.sipspot.websocket;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LocationRateLimiterTest {

    @Test
    void 첫_전송은_항상_허용된다() {
        LocationRateLimiter limiter = new LocationRateLimiter();
        assertThat(limiter.tryAcquire(UUID.randomUUID())).isTrue();
    }

    @Test
    void 인터벌_내_재전송은_차단된다() {
        UUID memberId = UUID.randomUUID();
        // 현재 시간을 고정해서 두 번 모두 같은 ms로 요청
        LocationRateLimiter limiter = new LocationRateLimiter() {
            private long time = 1000L;
            @Override protected long currentTimeMs() { return time; }
        };

        assertThat(limiter.tryAcquire(memberId)).isTrue();
        assertThat(limiter.tryAcquire(memberId)).isFalse(); // 같은 ms → 차단
    }

    @Test
    void 인터벌_경과_후_재전송은_허용된다() {
        UUID memberId = UUID.randomUUID();
        long[] time = {1000L};
        LocationRateLimiter limiter = new LocationRateLimiter() {
            @Override protected long currentTimeMs() { return time[0]; }
        };

        assertThat(limiter.tryAcquire(memberId)).isTrue();

        time[0] += LocationRateLimiter.MIN_INTERVAL_MS; // 500ms 경과
        assertThat(limiter.tryAcquire(memberId)).isTrue();
    }

    @Test
    void 다른_멤버는_서로_독립적으로_제한된다() {
        long[] time = {1000L};
        LocationRateLimiter limiter = new LocationRateLimiter() {
            @Override protected long currentTimeMs() { return time[0]; }
        };
        UUID member1 = UUID.randomUUID();
        UUID member2 = UUID.randomUUID();

        assertThat(limiter.tryAcquire(member1)).isTrue();
        assertThat(limiter.tryAcquire(member2)).isTrue(); // 다른 멤버는 독립적
        assertThat(limiter.tryAcquire(member1)).isFalse(); // member1만 차단
    }
}
