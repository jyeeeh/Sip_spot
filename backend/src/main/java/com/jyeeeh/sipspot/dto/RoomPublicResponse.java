package com.jyeeeh.sipspot.dto;

public record RoomPublicResponse(
        String code,
        String hostNickname,
        String location,
        boolean online,
        int viewerCount
) {}
