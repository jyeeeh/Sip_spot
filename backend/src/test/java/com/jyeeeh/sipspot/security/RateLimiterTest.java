package com.jyeeeh.sipspot.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    @Test
    void 커피삭제_처음에는_차단되지_않는다() {
        RateLimiter limiter = new RateLimiter();
        assertThat(limiter.isCoffeeDeleteBlocked("1.2.3.4")).isFalse();
    }

    @Test
    void 커피삭제_10회_미만은_차단되지_않는다() {
        RateLimiter limiter = new RateLimiter();
        String ip = "1.2.3.4";
        for (int i = 0; i < 9; i++) {
            limiter.recordCoffeeDelete(ip);
            assertThat(limiter.isCoffeeDeleteBlocked(ip)).isFalse();
        }
    }

    @Test
    void 커피삭제_10회_도달하면_차단된다() {
        RateLimiter limiter = new RateLimiter();
        String ip = "1.2.3.4";
        for (int i = 0; i < 10; i++) {
            limiter.recordCoffeeDelete(ip);
        }
        assertThat(limiter.isCoffeeDeleteBlocked(ip)).isTrue();
    }

    @Test
    void 커피삭제_다른_IP는_독립적으로_제한된다() {
        RateLimiter limiter = new RateLimiter();
        String ip1 = "1.2.3.4";
        String ip2 = "5.6.7.8";
        for (int i = 0; i < 10; i++) {
            limiter.recordCoffeeDelete(ip1);
        }
        assertThat(limiter.isCoffeeDeleteBlocked(ip1)).isTrue();
        assertThat(limiter.isCoffeeDeleteBlocked(ip2)).isFalse();
    }
}
