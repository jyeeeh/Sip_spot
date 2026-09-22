package com.jyeeeh.sipspot.websocket;

import com.jyeeeh.sipspot.domain.Account;
import com.jyeeeh.sipspot.domain.AccountSession;
import com.jyeeeh.sipspot.domain.Room;
import com.jyeeeh.sipspot.repository.AccountSessionRepository;
import com.jyeeeh.sipspot.repository.RoomRepository;
import com.jyeeeh.sipspot.service.PresenceRegistry;
import com.jyeeeh.sipspot.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StompChannelInterceptorTest {

    private TokenService tokenService;
    private AccountSessionRepository sessionRepo;
    private RoomRepository roomRepo;
    private PresenceRegistry presenceRegistry;
    private StompChannelInterceptor interceptor;

    private static final String VALID_TOKEN = "valid-token";
    private static final String VALID_HASH = "valid-hash";
    private static final String ROOM_CODE = "ABC1234";
    private static final Long ACCOUNT_ID = 1L;

    @BeforeEach
    void setUp() {
        tokenService = mock(TokenService.class);
        sessionRepo = mock(AccountSessionRepository.class);
        roomRepo = mock(RoomRepository.class);
        presenceRegistry = mock(PresenceRegistry.class);
        interceptor = new StompChannelInterceptor(sessionRepo, tokenService, roomRepo, presenceRegistry);

        when(tokenService.hash(VALID_TOKEN)).thenReturn(VALID_HASH);

        Account account = mock(Account.class);
        when(account.getId()).thenReturn(ACCOUNT_ID);

        AccountSession session = mock(AccountSession.class);
        when(session.getAccount()).thenReturn(account);

        Room room = mock(Room.class);
        when(room.getCode()).thenReturn(ROOM_CODE);

        when(sessionRepo.findByTokenHashWithAccount(VALID_HASH)).thenReturn(Optional.of(session));
        when(roomRepo.findByHostAccountId(ACCOUNT_ID)).thenReturn(Optional.of(room));
    }

    // ── CONNECT ────────────────────────────────────────────────────────────────

    @Test
    void CONNECT_토큰_없으면_익명으로_허용() {
        Message<?> msg = buildConnect(null);
        Message<?> result = interceptor.preSend(msg, null);
        assertThat(result).isNotNull();
        assertThat(StompHeaderAccessor.wrap(result).getUser()).isNull();
    }

    @Test
    void CONNECT_유효한_토큰이면_Principal_설정() {
        Message<?> msg = buildConnect("Bearer " + VALID_TOKEN);
        Message<?> result = interceptor.preSend(msg, null);

        AccountPrincipal principal = (AccountPrincipal) StompHeaderAccessor.wrap(result).getUser();
        assertThat(principal.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(principal.roomCode()).isEqualTo(ROOM_CODE);
    }

    @Test
    void CONNECT_방_없는_계정이면_roomCode_null() {
        when(roomRepo.findByHostAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());
        Message<?> msg = buildConnect("Bearer " + VALID_TOKEN);
        Message<?> result = interceptor.preSend(msg, null);

        AccountPrincipal principal = (AccountPrincipal) StompHeaderAccessor.wrap(result).getUser();
        assertThat(principal.roomCode()).isNull();
    }

    @Test
    void CONNECT_유효하지_않은_토큰이면_거부() {
        when(sessionRepo.findByTokenHashWithAccount(any())).thenReturn(Optional.empty());
        Message<?> msg = buildConnect("Bearer bad-token");
        assertThatThrownBy(() -> interceptor.preSend(msg, null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void CONNECT_Bearer_형식_아니면_거부() {
        Message<?> msg = buildConnect("Token " + VALID_TOKEN);
        assertThatThrownBy(() -> interceptor.preSend(msg, null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    // ── SUBSCRIBE ──────────────────────────────────────────────────────────────

    @Test
    void SUBSCRIBE_인증_없어도_방_코드_구독_허용() {
        Message<?> msg = buildSubscribe("/topic/rooms/" + ROOM_CODE, null);
        Message<?> result = interceptor.preSend(msg, null);
        assertThat(result).isNotNull();
    }

    @Test
    void SUBSCRIBE_허용되지_않은_목적지면_거부() {
        Message<?> msg = buildSubscribe("/topic/other", null);
        assertThatThrownBy(() -> interceptor.preSend(msg, null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void SUBSCRIBE_익명이면_accountId_null로_addViewer_호출() {
        Message<?> msg = buildSubscribe("/topic/rooms/" + ROOM_CODE, null);
        interceptor.preSend(msg, null);
        verify(presenceRegistry).addViewer(any(), eq(ROOM_CODE), isNull());
    }

    @Test
    void SUBSCRIBE_인증_사용자면_accountId로_addViewer_호출() {
        AccountPrincipal principal = new AccountPrincipal(ACCOUNT_ID, ROOM_CODE);
        Message<?> msg = buildSubscribe("/topic/rooms/" + ROOM_CODE, principal);
        interceptor.preSend(msg, null);
        verify(presenceRegistry).addViewer(any(), eq(ROOM_CODE), eq(ACCOUNT_ID));
    }

    @Test
    void SUBSCRIBE_소문자_코드도_대문자_정규화_후_addViewer_호출() {
        Message<?> msg = buildSubscribe("/topic/rooms/" + ROOM_CODE.toLowerCase(), null);
        interceptor.preSend(msg, null);
        verify(presenceRegistry).addViewer(any(), eq(ROOM_CODE), isNull());
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private Message<?> buildConnect(String authHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authHeader != null) accessor.addNativeHeader("Authorization", authHeader);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<?> buildSubscribe(String destination, AccountPrincipal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        if (principal != null) accessor.setUser(principal);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
