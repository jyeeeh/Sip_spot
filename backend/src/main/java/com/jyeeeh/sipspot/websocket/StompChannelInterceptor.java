package com.jyeeeh.sipspot.websocket;

import com.jyeeeh.sipspot.domain.AccountSession;
import com.jyeeeh.sipspot.repository.AccountSessionRepository;
import com.jyeeeh.sipspot.repository.RoomRepository;
import com.jyeeeh.sipspot.service.PresenceRegistry;
import com.jyeeeh.sipspot.service.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Component
public class StompChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompChannelInterceptor.class);
    private static final String TOPIC_ROOMS_PREFIX = "/topic/rooms/";

    private final AccountSessionRepository sessionRepo;
    private final TokenService tokenService;
    private final RoomRepository roomRepo;
    private final PresenceRegistry presenceRegistry;

    @Autowired
    public StompChannelInterceptor(AccountSessionRepository sessionRepo,
                                   TokenService tokenService,
                                   RoomRepository roomRepo,
                                   @Lazy PresenceRegistry presenceRegistry) {
        this.sessionRepo = sessionRepo;
        this.tokenService = tokenService;
        this.roomRepo = roomRepo;
        this.presenceRegistry = presenceRegistry;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        try {
            StompHeaderAccessor accessor =
                    MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor == null) return message;

            StompCommand command = accessor.getCommand();
            if (command == null) return message;

            return switch (command) {
                case CONNECT -> handleConnect(message, accessor);
                case SUBSCRIBE -> handleSubscribe(message, accessor);
                default -> message;
            };
        } catch (MessageDeliveryException e) {
            throw e;
        } catch (Exception e) {
            log.error("STOMP preSend 처리 중 예상치 못한 예외 발생", e);
            throw new MessageDeliveryException(message, e);
        }
    }

    private Message<?> handleConnect(Message<?> message, StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        List<String> authHeaders = accessor.getNativeHeader("Authorization");

        // Authorization 헤더가 없으면 익명 연결 허용
        if (authHeaders == null || authHeaders.isEmpty()) {
            log.debug("STOMP CONNECT 허용 — 익명 (sessionId={})", sessionId);
            return message;
        }

        String token = extractBearerToken(authHeaders);
        if (token == null) {
            log.warn("STOMP CONNECT 거부 — Authorization 헤더 형식 오류 (sessionId={})", sessionId);
            throw new MessageDeliveryException(message,
                    new IllegalArgumentException("Authorization 헤더 형식이 잘못됐습니다."));
        }

        String tokenHash = tokenService.hash(token);
        Optional<AccountSession> sessionOpt = sessionRepo.findByTokenHashWithAccount(tokenHash);
        if (sessionOpt.isEmpty()) {
            log.warn("STOMP CONNECT 거부 — 유효하지 않은 토큰 (sessionId={})", sessionId);
            throw new MessageDeliveryException(message,
                    new IllegalArgumentException("인증 실패"));
        }

        AccountSession accountSession = sessionOpt.get();
        Long accountId = accountSession.getAccount().getId();

        // 이 계정이 호스트인 방 코드 조회 (없으면 null)
        String roomCode = roomRepo.findByHostAccountId(accountId)
                .map(r -> r.getCode())
                .orElse(null);

        accessor.setUser(new AccountPrincipal(accountId, roomCode));
        log.debug("STOMP CONNECT 허용 — accountId={}, roomCode={}, sessionId={}", accountId, roomCode, sessionId);
        return message;
    }

    private Message<?> handleSubscribe(Message<?> message, StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        String destination = accessor.getDestination();

        if (destination == null || !destination.startsWith(TOPIC_ROOMS_PREFIX)) {
            log.warn("STOMP SUBSCRIBE 거부 — 허용되지 않은 목적지: {} (sessionId={})", destination, sessionId);
            throw new MessageDeliveryException(message,
                    new IllegalArgumentException("허용되지 않은 구독 목적지: " + destination));
        }

        String roomCode = destination.substring(TOPIC_ROOMS_PREFIX.length()).toUpperCase();

        // 구독자 accountId 추출 (익명이면 null)
        Principal user = accessor.getUser();
        Long accountId = (user instanceof AccountPrincipal p) ? p.accountId() : null;

        presenceRegistry.addViewer(sessionId, roomCode, accountId);
        log.debug("STOMP SUBSCRIBE 허용 — dest={}, accountId={}, sessionId={}", destination, accountId, sessionId);
        return message;
    }

    private String extractBearerToken(List<String> headers) {
        if (headers == null || headers.isEmpty()) return null;
        String header = headers.get(0);
        if (header == null || !header.startsWith("Bearer ")) return null;
        String token = header.substring(7).trim();
        return token.isEmpty() ? null : token;
    }
}
