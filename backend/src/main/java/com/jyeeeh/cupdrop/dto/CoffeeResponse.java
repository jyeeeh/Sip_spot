package com.jyeeeh.cupdrop.dto;

import com.jyeeeh.cupdrop.domain.Coffee;

import java.time.OffsetDateTime;

// 체크리스트 7: passwordHash 필드 없음 — API 응답에 민감 데이터 포함 금지
public record CoffeeResponse(Long id, String name, String message, String status, OffsetDateTime createdAt) {

    public static CoffeeResponse from(Coffee c) {
        return new CoffeeResponse(c.getId(), c.getWriterName(), c.getMessage(), c.getStatus(), c.getCreatedAt());
    }
}
