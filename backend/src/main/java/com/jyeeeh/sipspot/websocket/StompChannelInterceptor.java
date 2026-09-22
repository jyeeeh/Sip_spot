package com.jyeeeh.sipspot.websocket;

import com.jyeeeh.sipspot.repository.MemberRepository;
import com.jyeeeh.sipspot.service.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Component
public class StompChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompChannelInterceptor.class);
    private static final String TOPIC_ROOMS_PREFIX = "/topic/rooms/";

    private final TokenService tokenService;
    private final MemberRepository memberRepository;

    public StompChannelInterceptor(TokenService tokenService, MemberRepository memberRepository) {
        this.tokenService = tokenService;
        this.memberRepository = memberRepository;
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
            // 의도적 거부 — 이미 handleConnect/handleSubscribe 안에서 warn 로그를 남겼음
            throw e;
        } catch (Exception e) {
            // 예상치 못한 예외 — 스택트레이스 전체 기록
            log.error("STOMP preSend 처리 중 예상치 못한 예외 발생", e);
            throw new MessageDeliveryException(message, e);
        }
    }

    private Message<?> handleConnect(Message<?> message, StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        List<String> authHeaders = accessor.getNativeHeader("Authorization");

        // ① Authorization 헤더 자체가 없음
        if (authHeaders == null || authHeaders.isEmpty()) {
            log.warn("STOMP CONNECT 거부 — Authorization 헤더 없음 (sessionId={})", sessionId);
            throw new MessageDeliveryException("Authorization 헤더가 없습니다.");
        }

        // ② 헤더는 있으나 Bearer 형식이 아님
        String token = extractBearerToken(authHeaders);
        if (token == null) {
            log.warn("STOMP CONNECT 거부 — Authorization 헤더 형식 오류: 'Bearer ' 접두사 없음 (sessionId={})", sessionId);
            throw new MessageDeliveryException("Authorization 헤더 형식이 잘못됐습니다.");
        }

        // ③ 토큰은 있으나 DB에서 token_hash로 멤버를 찾지 못함 (토큰 원문은 로그에 남기지 않음)
        String tokenHash = tokenService.hash(token);
        var memberOpt = memberRepository.findByTokenHashWithRoom(tokenHash);
        if (memberOpt.isEmpty()) {
            log.warn("STOMP CONNECT 거부 — 토큰 해시에 해당하는 멤버 없음 (sessionId={})", sessionId);
            throw new MessageDeliveryException("인증 실패");
        }

        var member = memberOpt.get();
        String roomCode = member.getRoom().getCode().toUpperCase();
        accessor.setUser(new MemberPrincipal(member.getId(), roomCode));
        log.debug("STOMP CONNECT 허용 — memberId={}, roomCode={}, sessionId={}", member.getId(), roomCode, sessionId);
        return message;
    }

    private Message<?> handleSubscribe(Message<?> message, StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        Principal user = accessor.getUser();
        if (!(user instanceof MemberPrincipal principal)) {
            log.warn("STOMP SUBSCRIBE 거부 — Principal 없음 (sessionId={})", sessionId);
            throw new MessageDeliveryException("인증되지 않은 사용자");
        }

        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(TOPIC_ROOMS_PREFIX)) {
            log.warn("STOMP SUBSCRIBE 거부 — 허용되지 않은 목적지: {} (memberId={})", destination, principal.memberId());
            throw new MessageDeliveryException("허용되지 않은 구독 목적지: " + destination);
        }

        String codeInDest = destination.substring(TOPIC_ROOMS_PREFIX.length()).toUpperCase();
        if (!codeInDest.equals(principal.roomCode())) {
            log.warn("STOMP SUBSCRIBE 거부 — 다른 방 구독 시도: dest={}, memberId={}", destination, principal.memberId());
            throw new MessageDeliveryException("다른 방 구독 불가");
        }

        log.debug("STOMP SUBSCRIBE 허용 — dest={}, memberId={}", destination, principal.memberId());
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
