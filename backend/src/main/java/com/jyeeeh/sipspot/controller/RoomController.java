package com.jyeeeh.sipspot.controller;

import com.jyeeeh.sipspot.domain.AccountSession;
import com.jyeeeh.sipspot.dto.RoomPublicResponse;
import com.jyeeeh.sipspot.security.SessionResolver;
import com.jyeeeh.sipspot.service.AccountService;
import com.jyeeeh.sipspot.service.RoomService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final SessionResolver sessionResolver;

    public RoomController(RoomService roomService, SessionResolver sessionResolver) {
        this.roomService = roomService;
        this.sessionResolver = sessionResolver;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomService.CreateRoomResult createRoom(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AccountSession session = sessionResolver.resolveFromHeader(authHeader)
                .orElseThrow(AccountService.UnauthorizedException::new);
        return roomService.createRoom(session.getAccount().getId());
    }

    @GetMapping("/{code}")
    public RoomPublicResponse getRoom(@PathVariable String code) {
        return roomService.getRoom(code);
    }
}
