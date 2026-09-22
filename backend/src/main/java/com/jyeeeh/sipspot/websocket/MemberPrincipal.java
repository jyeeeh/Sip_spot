package com.jyeeeh.sipspot.websocket;

import java.security.Principal;
import java.util.UUID;

public record MemberPrincipal(UUID memberId, String roomCode) implements Principal {

    @Override
    public String getName() {
        return memberId.toString();
    }
}
