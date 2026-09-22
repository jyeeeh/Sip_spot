package com.jyeeeh.sipspot.security;

import com.jyeeeh.sipspot.domain.AccountSession;
import com.jyeeeh.sipspot.repository.AccountSessionRepository;
import com.jyeeeh.sipspot.service.TokenService;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SessionResolver {

    private final AccountSessionRepository sessionRepo;
    private final TokenService tokenService;

    public SessionResolver(AccountSessionRepository sessionRepo, TokenService tokenService) {
        this.sessionRepo = sessionRepo;
        this.tokenService = tokenService;
    }

    /**
     * Bearer 토큰 원문 → AccountSession (account eager loaded). 없으면 empty.
     */
    public Optional<AccountSession> resolve(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return Optional.empty();
        String tokenHash = tokenService.hash(rawToken);
        return sessionRepo.findByTokenHashWithAccount(tokenHash);
    }

    /**
     * "Bearer {token}" 형식의 Authorization 헤더값 → AccountSession.
     */
    public Optional<AccountSession> resolveFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return Optional.empty();
        String token = authHeader.substring(7).trim();
        return token.isEmpty() ? Optional.empty() : resolve(token);
    }
}
