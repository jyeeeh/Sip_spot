package com.jyeeeh.sipspot.dto.ws;

// 방에 호스트가 한 명이므로 신원 식별 필드(memberId) 제거
public record PresenceChangedEvent(String type, boolean online) {
    public PresenceChangedEvent(boolean online) {
        this("PRESENCE_CHANGED", online);
    }
}
