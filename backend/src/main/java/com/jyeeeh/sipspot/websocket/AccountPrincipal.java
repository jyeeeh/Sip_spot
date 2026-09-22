package com.jyeeeh.sipspot.websocket;

import java.security.Principal;

/**
 * @param roomCode 이 계정이 호스트인 방 코드. 방이 없으면 null.
 */
public record AccountPrincipal(Long accountId, String roomCode) implements Principal {
    @Override
    public String getName() { return accountId.toString(); }
}
