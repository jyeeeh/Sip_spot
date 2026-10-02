package com.jyeeeh.cupdrop.controller;

import com.jyeeeh.cupdrop.domain.AccountSession;
import com.jyeeeh.cupdrop.dto.CoffeeResponse;
import com.jyeeeh.cupdrop.dto.DeleteCoffeeRequest;
import com.jyeeeh.cupdrop.dto.SendCoffeeRequest;
import com.jyeeeh.cupdrop.exception.RateLimitExceededException;
import com.jyeeeh.cupdrop.security.RateLimiter;
import com.jyeeeh.cupdrop.security.SessionResolver;
import com.jyeeeh.cupdrop.service.AccountService;
import com.jyeeeh.cupdrop.service.CoffeeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/{code}/coffees")
public class CoffeeController {

    private final CoffeeService coffeeService;
    private final SessionResolver sessionResolver;
    private final RateLimiter rateLimiter;

    public CoffeeController(CoffeeService coffeeService, SessionResolver sessionResolver,
                            RateLimiter rateLimiter) {
        this.coffeeService = coffeeService;
        this.sessionResolver = sessionResolver;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping
    public List<CoffeeResponse> getCoffees(@PathVariable String code) {
        return coffeeService.getCoffees(code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CoffeeResponse sendCoffee(
            @PathVariable String code,
            @Valid @RequestBody SendCoffeeRequest body,
            HttpServletRequest request) {
        // 체크리스트 9: 전송 레이트리밋 (IP/5초/1회)
        String ip = request.getRemoteAddr();
        if (rateLimiter.isCoffeeBlocked(ip)) throw new RateLimitExceededException();
        rateLimiter.recordCoffee(ip);
        return coffeeService.sendCoffee(code, body.name(), body.message(), body.password());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCoffee(
            @PathVariable String code,
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) DeleteCoffeeRequest body,
            HttpServletRequest request) {
        // 체크리스트 9: 삭제 레이트리밋 (IP/1분/10회) — 비밀번호 무차별 대입 방지
        String ip = request.getRemoteAddr();
        if (rateLimiter.isCoffeeDeleteBlocked(ip)) throw new RateLimitExceededException();
        rateLimiter.recordCoffeeDelete(ip);

        if (authHeader != null) {
            // 체크리스트 4: 호스트 경로 — 서버가 발급한 세션 토큰으로만 신원 확인
            AccountSession session = sessionResolver.resolveFromHeader(authHeader)
                    .orElseThrow(AccountService.UnauthorizedException::new);
            coffeeService.deleteCoffeeByHost(id, code, session.getAccount().getId());
        } else if (body != null && body.password() != null && !body.password().isBlank()) {
            // 비밀번호 경로
            coffeeService.deleteCoffeeByPassword(id, code, body.password());
        } else {
            // 둘 다 없으면 400
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Authorization 헤더 또는 비밀번호가 필요합니다.");
        }
    }
}
