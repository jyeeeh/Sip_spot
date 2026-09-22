package com.jyeeeh.sipspot.controller;

import com.jyeeeh.sipspot.domain.AccountSession;
import com.jyeeeh.sipspot.dto.auth.LoginRequest;
import com.jyeeeh.sipspot.dto.auth.LoginResponse;
import com.jyeeeh.sipspot.dto.auth.MeResponse;
import com.jyeeeh.sipspot.dto.auth.SignupRequest;
import com.jyeeeh.sipspot.dto.auth.SignupResponse;
import com.jyeeeh.sipspot.exception.RateLimitExceededException;
import com.jyeeeh.sipspot.security.RateLimiter;
import com.jyeeeh.sipspot.security.SessionResolver;
import com.jyeeeh.sipspot.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {

    private final AccountService accountService;
    private final RateLimiter rateLimiter;
    private final SessionResolver sessionResolver;

    public AuthController(AccountService accountService, RateLimiter rateLimiter,
                          SessionResolver sessionResolver) {
        this.accountService = accountService;
        this.rateLimiter = rateLimiter;
        this.sessionResolver = sessionResolver;
    }

    @PostMapping("/api/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponse signup(@Valid @RequestBody SignupRequest request,
                                 HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        if (rateLimiter.isSignupBlocked(ip)) throw new RateLimitExceededException();

        SignupResponse response = accountService.signup(
                request.username(), request.password(), request.nickname());
        rateLimiter.recordSignup(ip);
        return response;
    }

    @PostMapping("/api/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request,
                               HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        if (rateLimiter.isLoginBlocked(ip)) throw new RateLimitExceededException();

        try {
            LoginResponse response = accountService.login(request.username(), request.password());
            return response;
        } catch (AccountService.AuthException e) {
            rateLimiter.recordLogin(ip);
            throw e;
        }
    }

    @PostMapping("/api/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        sessionResolver.resolveFromHeader(authHeader)
                .ifPresent(s -> accountService.logout(s.getTokenHash()));
    }

    @GetMapping("/api/me")
    public MeResponse me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        AccountSession session = sessionResolver.resolveFromHeader(authHeader)
                .orElseThrow(AccountService.UnauthorizedException::new);
        return accountService.me(session.getAccount().getId());
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
