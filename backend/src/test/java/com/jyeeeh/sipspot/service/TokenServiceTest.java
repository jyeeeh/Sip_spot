package com.jyeeeh.sipspot.service;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {

    private final TokenService tokenService = new TokenService();

    @Test
    void 토큰은_base64url_형식이다() {
        String token = tokenService.generateToken();
        // base64url 디코딩이 성공해야 함
        byte[] decoded = Base64.getUrlDecoder().decode(token);
        assertThat(decoded).hasSize(32);
    }

    @Test
    void 토큰에는_패딩이_없다() {
        String token = tokenService.generateToken();
        assertThat(token).doesNotContain("=");
    }

    @Test
    void 동일_토큰은_동일_해시를_반환한다() {
        String token = tokenService.generateToken();
        assertThat(tokenService.hash(token)).isEqualTo(tokenService.hash(token));
    }

    @Test
    void 다른_토큰은_다른_해시를_반환한다() {
        String token1 = tokenService.generateToken();
        String token2 = tokenService.generateToken();
        assertThat(tokenService.hash(token1)).isNotEqualTo(tokenService.hash(token2));
    }

    @Test
    void 해시는_64자_hex이다() {
        String hash = tokenService.hash(tokenService.generateToken());
        assertThat(hash).hasSize(64).matches("[0-9a-f]+");
    }
}
