package com.jyeeeh.sipspot.dto.ws;

import java.time.Instant;
import java.util.UUID;

public record LocationChangedEvent(
        String type,
        UUID memberId,
        String nickname,
        String location,
        Instant at
) {
    public LocationChangedEvent(UUID memberId, String nickname, String location) {
        this("LOCATION_CHANGED", memberId, nickname, location, Instant.now());
    }
}
