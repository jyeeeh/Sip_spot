package com.jyeeeh.sipspot.websocket;

import com.jyeeeh.sipspot.domain.Location;
import com.jyeeeh.sipspot.domain.Member;
import com.jyeeeh.sipspot.domain.Room;
import com.jyeeeh.sipspot.repository.MemberRepository;
import com.jyeeeh.sipspot.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class StompChannelInterceptorTest {

    private TokenService tokenService;
    private MemberRepository memberRepository;
    private StompChannelInterceptor interceptor;

    private static final String VALID_TOKEN = "valid-token";
    private static final String VALID_HASH = "valid-hash";
    private static final String ROOM_CODE = "ABC1234";
    private static final UUID MEMBER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        tokenService = mock(TokenService.class);
        memberRepository = mock(MemberRepository.class);
        interceptor = new StompChannelInterceptor(tokenService, memberRepository);

        when(tokenService.hash(VALID_TOKEN)).thenReturn(VALID_HASH);

        Room room = mock(Room.class);
        when(room.getCode()).thenReturn(ROOM_CODE);

        Member member = mock(Member.class);
        when(member.getId()).thenReturn(MEMBER_ID);
        when(member.getRoom()).thenReturn(room);

        when(memberRepository.findByTokenHash(VALID_HASH)).thenReturn(Optional.of(member));
    }

    // ── CONNECT ────────────────────────────────────────────────────────────────

    @Test
    void CONNECT_토큰_없으면_거부() {
        Message<?> msg = buildConnect(null);
        assertThatThrownBy(() -> interceptor.preSend(msg, null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void CONNECT_잘못된_토큰이면_거부() {
        when(memberRepository.findByTokenHash(any())).thenReturn(Optional.empty());
        Message<?> msg = buildConnect("Bearer bad-token");
        assertThatThrownBy(() -> interceptor.preSend(msg, null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void CONNECT_유효한_토큰이면_Principal_설정() {
        Message<?> msg = buildConnect("Bearer " + VALID_TOKEN);
        Message<?> result = interceptor.preSend(msg, null);

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertThat(resultAccessor.getUser()).isInstanceOf(MemberPrincipal.class);
        MemberPrincipal principal = (MemberPrincipal) resultAccessor.getUser();
        assertThat(principal.memberId()).isEqualTo(MEMBER_ID);
        assertThat(principal.roomCode()).isEqualTo(ROOM_CODE);
    }

    // ── SUBSCRIBE ──────────────────────────────────────────────────────────────

    @Test
    void SUBSCRIBE_Principal_없으면_거부() {
        Message<?> msg = buildSubscribe("/topic/rooms/" + ROOM_CODE, null);
        assertThatThrownBy(() -> interceptor.preSend(msg, null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void SUBSCRIBE_다른_방_코드이면_거부() {
        MemberPrincipal principal = new MemberPrincipal(MEMBER_ID, ROOM_CODE);
        Message<?> msg = buildSubscribe("/topic/rooms/ZZZZZZZ", principal);
        assertThatThrownBy(() -> interceptor.preSend(msg, null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void SUBSCRIBE_허용되지_않은_목적지면_거부() {
        MemberPrincipal principal = new MemberPrincipal(MEMBER_ID, ROOM_CODE);
        Message<?> msg = buildSubscribe("/topic/other", principal);
        assertThatThrownBy(() -> interceptor.preSend(msg, null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void SUBSCRIBE_자기_방_코드면_허용() {
        MemberPrincipal principal = new MemberPrincipal(MEMBER_ID, ROOM_CODE);
        Message<?> msg = buildSubscribe("/topic/rooms/" + ROOM_CODE, principal);
        Message<?> result = interceptor.preSend(msg, null);
        assertThat(result).isNotNull();
    }

    @Test
    void SUBSCRIBE_소문자_코드도_대문자_정규화_후_허용() {
        MemberPrincipal principal = new MemberPrincipal(MEMBER_ID, ROOM_CODE);
        Message<?> msg = buildSubscribe("/topic/rooms/" + ROOM_CODE.toLowerCase(), principal);
        Message<?> result = interceptor.preSend(msg, null);
        assertThat(result).isNotNull();
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private Message<?> buildConnect(String authHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authHeader != null) accessor.addNativeHeader("Authorization", authHeader);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<?> buildSubscribe(String destination, MemberPrincipal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        if (principal != null) accessor.setUser(principal);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
