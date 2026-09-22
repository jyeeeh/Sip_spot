package com.jyeeeh.sipspot.service;

import com.jyeeeh.sipspot.websocket.MemberPrincipal;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
public class PresenceEventListener {

    private final PresenceRegistry presenceRegistry;

    public PresenceEventListener(PresenceRegistry presenceRegistry) {
        this.presenceRegistry = presenceRegistry;
    }

    @EventListener
    public void onSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = sha.getUser();
        if (!(user instanceof MemberPrincipal principal)) return;

        String sessionId = sha.getSessionId();
        if (sessionId == null) return;

        presenceRegistry.onConnect(sessionId, principal.memberId(), principal.roomCode());
    }

    @EventListener
    public void onSessionDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = sha.getUser();
        if (!(user instanceof MemberPrincipal principal)) return;

        String sessionId = sha.getSessionId();
        if (sessionId == null) return;

        presenceRegistry.onDisconnect(sessionId, principal.memberId());
    }
}
