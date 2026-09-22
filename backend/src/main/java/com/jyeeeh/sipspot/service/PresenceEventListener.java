package com.jyeeeh.sipspot.service;

import com.jyeeeh.sipspot.websocket.AccountPrincipal;
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
        if (!(user instanceof AccountPrincipal principal)) return;
        if (principal.roomCode() == null) return; // 방 없는 계정은 presence 추적 불필요

        String sessionId = sha.getSessionId();
        if (sessionId == null) return;

        presenceRegistry.onConnect(sessionId, principal.accountId(), principal.roomCode());
    }

    @EventListener
    public void onSessionDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = sha.getSessionId();
        if (sessionId == null) return;

        // 모든 세션(호스트·게스트 무관)의 뷰어 추적 정리
        presenceRegistry.removeViewerSessions(sessionId);

        // 인증된 세션(호스트)만 presence 추적 정리
        Principal user = sha.getUser();
        if (user instanceof AccountPrincipal principal) {
            presenceRegistry.onDisconnect(sessionId, principal.accountId());
        }
    }
}
