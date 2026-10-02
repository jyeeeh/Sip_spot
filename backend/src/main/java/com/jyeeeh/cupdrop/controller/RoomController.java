package com.jyeeeh.cupdrop.controller;

import com.jyeeeh.cupdrop.domain.AccountSession;
import com.jyeeeh.cupdrop.dto.RoomPublicResponse;
import com.jyeeeh.cupdrop.security.SessionResolver;
import com.jyeeeh.cupdrop.service.AccountService;
import com.jyeeeh.cupdrop.service.RoomService;
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
