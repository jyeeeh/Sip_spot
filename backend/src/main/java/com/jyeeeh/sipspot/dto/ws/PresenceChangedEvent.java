package com.jyeeeh.sipspot.dto.ws;

import java.util.UUID;

public record PresenceChangedEvent(
        String type,
        UUID memberId,
        boolean online
) {
    public PresenceChangedEvent(UUID memberId, boolean online) {
        this("PRESENCE_CHANGED", memberId, online);
    }
}
