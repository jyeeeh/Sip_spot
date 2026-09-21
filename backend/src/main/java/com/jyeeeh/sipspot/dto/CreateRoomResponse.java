package com.jyeeeh.sipspot.dto;

import java.util.UUID;

public record CreateRoomResponse(String code, UUID memberId, String token) {}
