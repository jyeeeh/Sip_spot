package com.jyeeeh.sipspot.service;

import com.jyeeeh.sipspot.dto.ws.PresenceChangedEvent;
import com.jyeeeh.sipspot.dto.ws.ViewerCountChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PresenceRegistryTest {

    private SimpMessagingTemplate messagingTemplate;
    private ThreadPoolTaskScheduler taskScheduler;
    private PresenceRegistry registry;

    private static final String ROOM_CODE = "ABC1234";
    private static final Long ACCOUNT_ID = 1L;
    private static final String SESSION_1 = "session-1";
    private static final String SESSION_2 = "session-2";
    private static final String GUEST_SESSION = "guest-session";

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.initialize();
        // 지연 0ms로 즉시 실행 (테스트용)
        registry = new PresenceRegistry(messagingTemplate, taskScheduler, 0L);
    }

    // ── 호스트 presence ────────────────────────────────────────────────────────

    @Test
    void 첫_세션_연결_시_온라인_브로드캐스트() {
        registry.onConnect(SESSION_1, ACCOUNT_ID, ROOM_CODE);

        assertThat(registry.isOnline(ACCOUNT_ID)).isTrue();
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/rooms/" + ROOM_CODE), captor.capture());
        PresenceChangedEvent event = (PresenceChangedEvent) captor.getValue();
        assertThat(event.online()).isTrue();
    }

    @Test
    void 두번째_세션_연결_시_브로드캐스트_없음() {
        registry.onConnect(SESSION_1, ACCOUNT_ID, ROOM_CODE);
        clearInvocations(messagingTemplate);

        registry.onConnect(SESSION_2, ACCOUNT_ID, ROOM_CODE);

        assertThat(registry.isOnline(ACCOUNT_ID)).isTrue();
        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }

    @Test
    void 세션_하나_남은_상태에서_해제해도_여전히_온라인() throws InterruptedException {
        registry.onConnect(SESSION_1, ACCOUNT_ID, ROOM_CODE);
        registry.onConnect(SESSION_2, ACCOUNT_ID, ROOM_CODE);
        clearInvocations(messagingTemplate);

        registry.onDisconnect(SESSION_1, ACCOUNT_ID);
        Thread.sleep(50);

        assertThat(registry.isOnline(ACCOUNT_ID)).isTrue();
        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }

    @Test
    void 마지막_세션_해제_후_오프라인_브로드캐스트() throws InterruptedException {
        registry.onConnect(SESSION_1, ACCOUNT_ID, ROOM_CODE);
        clearInvocations(messagingTemplate);

        registry.onDisconnect(SESSION_1, ACCOUNT_ID);
        Thread.sleep(50);

        assertThat(registry.isOnline(ACCOUNT_ID)).isFalse();
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/rooms/" + ROOM_CODE), captor.capture());
        PresenceChangedEvent event = (PresenceChangedEvent) captor.getValue();
        assertThat(event.online()).isFalse();
    }

    @Test
    void 같은_계정의_다중_세션_중_하나만_끊겨도_온라인_유지() throws InterruptedException {
        // 같은 accountId로 두 브라우저가 각각 WebSocket 연결
        registry.onConnect(SESSION_1, ACCOUNT_ID, ROOM_CODE);
        registry.onConnect(SESSION_2, ACCOUNT_ID, ROOM_CODE);
        clearInvocations(messagingTemplate);

        // 첫 번째 브라우저만 끊김
        registry.onDisconnect(SESSION_1, ACCOUNT_ID);
        Thread.sleep(50); // 지연 0ms이므로 충분

        // 두 번째 브라우저가 살아있으므로 온라인 상태 유지
        assertThat(registry.isOnline(ACCOUNT_ID)).isTrue();
        // 오프라인 브로드캐스트가 나가면 안 됨
        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }

    @Test
    void 해제_후_재연결_시_오프라인_브로드캐스트_취소() throws InterruptedException {
        PresenceRegistry slowRegistry = new PresenceRegistry(messagingTemplate, taskScheduler, 50L);
        slowRegistry.onConnect(SESSION_1, ACCOUNT_ID, ROOM_CODE);
        clearInvocations(messagingTemplate);

        slowRegistry.onDisconnect(SESSION_1, ACCOUNT_ID);
        slowRegistry.onConnect(SESSION_2, ACCOUNT_ID, ROOM_CODE);
        Thread.sleep(100);

        assertThat(slowRegistry.isOnline(ACCOUNT_ID)).isTrue();
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate, atMost(1)).convertAndSend(
                eq("/topic/rooms/" + ROOM_CODE), captor.capture());
    }

    // ── 뷰어 카운트 ────────────────────────────────────────────────────────────

    @Test
    void 게스트_구독_시_뷰어_카운트_증가() {
        registry.addViewer(GUEST_SESSION, ROOM_CODE, null);

        assertThat(registry.getViewerCount(ROOM_CODE)).isEqualTo(1);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/rooms/" + ROOM_CODE), captor.capture());
        ViewerCountChangedEvent event = (ViewerCountChangedEvent) captor.getValue();
        assertThat(event.count()).isEqualTo(1);
    }

    @Test
    void 호스트_본인_구독은_뷰어_카운트_제외() {
        registry.onConnect(SESSION_1, ACCOUNT_ID, ROOM_CODE);
        clearInvocations(messagingTemplate);

        // 호스트 본인 구독
        registry.addViewer(SESSION_1, ROOM_CODE, ACCOUNT_ID);

        assertThat(registry.getViewerCount(ROOM_CODE)).isEqualTo(0);
        verify(messagingTemplate, never()).convertAndSend(
                eq("/topic/rooms/" + ROOM_CODE), any(ViewerCountChangedEvent.class));
    }

    @Test
    void 뷰어_연결_해제_시_카운트_감소_브로드캐스트() {
        registry.addViewer(GUEST_SESSION, ROOM_CODE, null);
        clearInvocations(messagingTemplate);

        registry.removeViewerSessions(GUEST_SESSION);

        assertThat(registry.getViewerCount(ROOM_CODE)).isEqualTo(0);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/rooms/" + ROOM_CODE), captor.capture());
        ViewerCountChangedEvent event = (ViewerCountChangedEvent) captor.getValue();
        assertThat(event.count()).isEqualTo(0);
    }

    @Test
    void 카운트_변화_없으면_브로드캐스트_없음() {
        // 아무도 구독 안 한 방에서 removeViewerSessions 호출
        registry.removeViewerSessions("unknown-session");

        verify(messagingTemplate, never()).convertAndSend(
                eq("/topic/rooms/" + ROOM_CODE), any(ViewerCountChangedEvent.class));
    }

    @Test
    void 여러_게스트_구독_후_순차_해제() {
        String guest1 = "guest-1";
        String guest2 = "guest-2";

        registry.addViewer(guest1, ROOM_CODE, null);
        registry.addViewer(guest2, ROOM_CODE, null);
        assertThat(registry.getViewerCount(ROOM_CODE)).isEqualTo(2);

        registry.removeViewerSessions(guest1);
        assertThat(registry.getViewerCount(ROOM_CODE)).isEqualTo(1);

        registry.removeViewerSessions(guest2);
        assertThat(registry.getViewerCount(ROOM_CODE)).isEqualTo(0);
    }
}
