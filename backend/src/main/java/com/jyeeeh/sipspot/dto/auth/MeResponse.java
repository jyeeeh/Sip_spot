package com.jyeeeh.sipspot.dto.auth;

public record MeResponse(Long accountId, String username, String nickname, RoomSummary room) {}
