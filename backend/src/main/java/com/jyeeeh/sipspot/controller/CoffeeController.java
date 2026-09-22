package com.jyeeeh.sipspot.controller;

import com.jyeeeh.sipspot.domain.AccountSession;
import com.jyeeeh.sipspot.dto.CoffeeResponse;
import com.jyeeeh.sipspot.dto.SendCoffeeRequest;
import com.jyeeeh.sipspot.exception.RateLimitExceededException;
import com.jyeeeh.sipspot.security.RateLimiter;
import com.jyeeeh.sipspot.security.SessionResolver;
import com.jyeeeh.sipspot.service.AccountService;
import com.jyeeeh.sipspot.service.CoffeeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
            @RequestBody(required = false) SendCoffeeRequest body,
            HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if (rateLimiter.isCoffeeBlocked(ip)) throw new RateLimitExceededException();
        rateLimiter.recordCoffee(ip);
        String message = (body != null) ? body.message() : null;
        return coffeeService.sendCoffee(code, message);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCoffee(
            @PathVariable String code,
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AccountSession session = sessionResolver.resolveFromHeader(authHeader)
                .orElseThrow(AccountService.UnauthorizedException::new);
        coffeeService.deleteCoffee(id, code, session.getAccount().getId());
    }
}
