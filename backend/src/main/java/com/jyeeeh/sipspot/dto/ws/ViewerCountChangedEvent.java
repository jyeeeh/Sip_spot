package com.jyeeeh.sipspot.dto.ws;

// 신원 정보 없음 — 숫자만 포함
public record ViewerCountChangedEvent(String type, int count) {
    public ViewerCountChangedEvent(int count) {
        this("VIEWER_COUNT_CHANGED", count);
    }
}
