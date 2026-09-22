package com.jyeeeh.sipspot.websocket;

import com.jyeeeh.sipspot.domain.Location;
import com.jyeeeh.sipspot.service.LocationService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class LocationMessageController {

    private final LocationService locationService;
    private final LocationRateLimiter rateLimiter;

    public LocationMessageController(LocationService locationService,
                                     LocationRateLimiter rateLimiter) {
        this.locationService = locationService;
        this.rateLimiter = rateLimiter;
    }

    @MessageMapping("/rooms/{code}/location")
    public void handleLocation(@DestinationVariable String code,
                               @Payload LocationPayload payload,
                               Principal principal) {
        if (!(principal instanceof AccountPrincipal accountPrincipal)) return;

        // URL code 대문자 정규화 후 Principal의 roomCode와 비교
        String normalizedCode = code.toUpperCase();
        if (!normalizedCode.equals(accountPrincipal.roomCode())) return;

        // location 값 enum 검증
        Location location;
        try {
            location = Location.valueOf(payload.location().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return; // 잘못된 값 무시
        }

        // 계정당 초당 2회 제한
        if (!rateLimiter.tryAcquire(accountPrincipal.accountId())) return;

        // DB 저장 + 브로드캐스트 (accountId는 Principal에서만)
        locationService.updateLocation(accountPrincipal.accountId(), normalizedCode, location);
    }

    public record LocationPayload(String location) {}
}
