package com.jyeeeh.sipspot.dto;

import java.util.UUID;

public record JoinRoomResponse(UUID memberId, String token) {}
