package com.jyeeeh.sipspot.service;

import com.jyeeeh.sipspot.dto.ws.PresenceChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PresenceRegistryTest {

    private SimpMessagingTemplate messagingTemplate;
    private ThreadPoolTaskScheduler taskScheduler;
    private PresenceRegistry registry;

    private static final String ROOM_CODE = "ABC1234";
    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final String SESSION_1 = "session-1";
    private static final String SESSION_2 = "session-2";

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.initialize();
        // 지연 0ms로 즉시 실행 (테스트용)
        registry = new PresenceRegistry(messagingTemplate, taskScheduler, 0L);
    }

    @Test
    void 첫_세션_연결_시_온라인_브로드캐스트() {
        registry.onConnect(SESSION_1, MEMBER_ID, ROOM_CODE);

        assertThat(registry.isOnline(MEMBER_ID)).isTrue();
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/rooms/" + ROOM_CODE), captor.capture());
        PresenceChangedEvent event = (PresenceChangedEvent) captor.getValue();
        assertThat(event.online()).isTrue();
        assertThat(event.memberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    void 두번째_세션_연결_시_브로드캐스트_없음() {
        registry.onConnect(SESSION_1, MEMBER_ID, ROOM_CODE);
        clearInvocations(messagingTemplate);

        registry.onConnect(SESSION_2, MEMBER_ID, ROOM_CODE);

        assertThat(registry.isOnline(MEMBER_ID)).isTrue();
        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }

    @Test
    void 세션_하나_남은_상태에서_해제해도_여전히_온라인() throws InterruptedException {
        registry.onConnect(SESSION_1, MEMBER_ID, ROOM_CODE);
        registry.onConnect(SESSION_2, MEMBER_ID, ROOM_CODE);
        clearInvocations(messagingTemplate);

        registry.onDisconnect(SESSION_1, MEMBER_ID);
        Thread.sleep(50); // 지연 0ms이므로 충분

        assertThat(registry.isOnline(MEMBER_ID)).isTrue();
        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }

    @Test
    void 마지막_세션_해제_후_오프라인_브로드캐스트() throws InterruptedException {
        registry.onConnect(SESSION_1, MEMBER_ID, ROOM_CODE);
        clearInvocations(messagingTemplate);

        registry.onDisconnect(SESSION_1, MEMBER_ID);
        Thread.sleep(50);

        assertThat(registry.isOnline(MEMBER_ID)).isFalse();
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/rooms/" + ROOM_CODE), captor.capture());
        PresenceChangedEvent event = (PresenceChangedEvent) captor.getValue();
        assertThat(event.online()).isFalse();
    }

    @Test
    void 해제_후_재연결_시_오프라인_브로드캐스트_취소() throws InterruptedException {
        // 지연 50ms 주어서 재연결 테스트
        PresenceRegistry slowRegistry = new PresenceRegistry(messagingTemplate, taskScheduler, 50L);
        slowRegistry.onConnect(SESSION_1, MEMBER_ID, ROOM_CODE);
        clearInvocations(messagingTemplate);

        slowRegistry.onDisconnect(SESSION_1, MEMBER_ID);
        // 50ms 지연 전에 재연결
        slowRegistry.onConnect(SESSION_2, MEMBER_ID, ROOM_CODE);
        Thread.sleep(100);

        // 재연결로 인해 온라인 상태 → 지연 후 체크에서 세션이 남아있으므로 오프라인 브로드캐스트 없음
        assertThat(slowRegistry.isOnline(MEMBER_ID)).isTrue();
        // 재연결 시 onConnect에서 online=true 브로드캐스트 1회만 있어야 함
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate, atMost(1)).convertAndSend(
                eq("/topic/rooms/" + ROOM_CODE), captor.capture());
    }
}
