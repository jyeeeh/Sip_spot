package com.jyeeeh.sipspot.dto.ws;

import java.util.UUID;

public record MemberJoinedEvent(
        String type,
        UUID memberId,
        String nickname,
        String location,
        boolean online
) {
    public MemberJoinedEvent(UUID memberId, String nickname, String location, boolean online) {
        this("MEMBER_JOINED", memberId, nickname, location, online);
    }
}
