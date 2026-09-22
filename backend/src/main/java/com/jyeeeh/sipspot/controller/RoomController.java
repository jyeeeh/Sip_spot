package com.jyeeeh.sipspot.controller;

import com.jyeeeh.sipspot.domain.Location;
import com.jyeeeh.sipspot.dto.CreateRoomRequest;
import com.jyeeeh.sipspot.dto.CreateRoomResponse;
import com.jyeeeh.sipspot.dto.JoinRoomRequest;
import com.jyeeeh.sipspot.dto.JoinRoomResponse;
import com.jyeeeh.sipspot.dto.RoomDetailResponse;
import com.jyeeeh.sipspot.dto.ws.MemberJoinedEvent;
import com.jyeeeh.sipspot.exception.RateLimitExceededException;
import com.jyeeeh.sipspot.security.RateLimiter;
import com.jyeeeh.sipspot.service.RoomService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final RateLimiter rateLimiter;
    private final SimpMessagingTemplate messagingTemplate;

    public RoomController(RoomService roomService, RateLimiter rateLimiter,
                          SimpMessagingTemplate messagingTemplate) {
        this.roomService = roomService;
        this.rateLimiter = rateLimiter;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateRoomResponse createRoom(@Valid @RequestBody CreateRoomRequest request,
                                         HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        if (rateLimiter.isCreateBlocked(ip)) throw new RateLimitExceededException();

        CreateRoomResponse response = roomService.createRoom(request.nickname().trim());
        rateLimiter.recordCreate(ip);
        return response;
    }

    @PostMapping("/{code}/join")
    @ResponseStatus(HttpStatus.CREATED)
    public JoinRoomResponse joinRoom(@PathVariable String code,
                                     @Valid @RequestBody JoinRoomRequest request,
                                     HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        if (rateLimiter.isJoinFailBlocked(ip)) throw new RateLimitExceededException();

        String normalizedCode = code.toUpperCase();
        JoinRoomResponse response;
        try {
            response = roomService.joinRoom(normalizedCode, request.nickname().trim());
        } catch (RoomService.RoomNotFoundException e) {
            rateLimiter.recordJoinFail(ip);
            throw e;
        }

        // 신규 멤버 입장을 실시간으로 브로드캐스트 (민감 필드 없음)
        MemberJoinedEvent event = new MemberJoinedEvent(
                response.memberId(),
                request.nickname().trim(),
                Location.LIVING_ROOM.name(), // 신규 멤버 기본 위치
                false                         // WS 미연결 상태로 시작
        );
        messagingTemplate.convertAndSend("/topic/rooms/" + normalizedCode, event);

        return response;
    }

    @GetMapping("/{code}")
    public RoomDetailResponse getRoom(@PathVariable String code,
                                      @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractBearerToken(authHeader);
        if (token == null) throw new RoomService.UnauthorizedException();
        return roomService.getRoom(code, token);
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
