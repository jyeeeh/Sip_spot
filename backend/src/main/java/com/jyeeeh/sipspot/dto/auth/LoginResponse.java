package com.jyeeeh.sipspot.dto.auth;

public record LoginResponse(Long accountId, String nickname, String token, RoomSummary room) {}
