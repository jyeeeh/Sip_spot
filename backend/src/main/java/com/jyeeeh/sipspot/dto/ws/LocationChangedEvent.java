package com.jyeeeh.sipspot.dto.ws;

import java.time.Instant;

// 방에 호스트가 한 명이므로 신원 식별 필드(memberId, nickname) 제거
public record LocationChangedEvent(String type, String location, Instant at) {
    public LocationChangedEvent(String location) {
        this("LOCATION_CHANGED", location, Instant.now());
    }
}
