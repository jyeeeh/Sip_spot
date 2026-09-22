package com.jyeeeh.sipspot.dto;

import com.jyeeeh.sipspot.domain.Coffee;

import java.time.OffsetDateTime;

public record CoffeeResponse(Long id, String message, String status, OffsetDateTime createdAt) {

    public static CoffeeResponse from(Coffee c) {
        return new CoffeeResponse(c.getId(), c.getMessage(), c.getStatus(), c.getCreatedAt());
    }
}
