package com.jyeeeh.sipspot.service;

import com.jyeeeh.sipspot.dto.ws.PresenceChangedEvent;
import com.jyeeeh.sipspot.dto.ws.ViewerCountChangedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class PresenceRegistry {

    private static final long OFFLINE_DELAY_MS = 3_000L;

    // ── 호스트 presence 추적 ────────────────────────────────────────────────────
    private final ConcurrentHashMap<Long, CopyOnWriteArraySet<String>> sessions =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> accountRooms = new ConcurrentHashMap<>();

    // ── 뷰어 카운트 추적 ────────────────────────────────────────────────────────
    /** roomCode → 뷰어 sessionId 집합 (호스트 본인 제외) */
    private final ConcurrentHashMap<String, CopyOnWriteArraySet<String>> viewerSessions =
            new ConcurrentHashMap<>();
    /** sessionId → 구독 중인 roomCode 집합 (DISCONNECT 시 역인덱스 정리용) */
    private final ConcurrentHashMap<String, Set<String>> sessionRooms =
            new ConcurrentHashMap<>();

    private final SimpMessagingTemplate messagingTemplate;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final long offlineDelayMs;

    @Autowired
    public PresenceRegistry(SimpMessagingTemplate messagingTemplate,
                            ThreadPoolTaskScheduler taskScheduler) {
        this(messagingTemplate, taskScheduler, OFFLINE_DELAY_MS);
    }

    public PresenceRegistry(SimpMessagingTemplate messagingTemplate,
                            ThreadPoolTaskScheduler taskScheduler,
                            long offlineDelayMs) {
        this.messagingTemplate = messagingTemplate;
        this.taskScheduler = taskScheduler;
        this.offlineDelayMs = offlineDelayMs;
    }

    // ── 호스트 presence ─────────────────────────────────────────────────────────

    public void onConnect(String sessionId, Long accountId, String roomCode) {
        accountRooms.put(accountId, roomCode);
        sessions.compute(accountId, (id, set) -> {
            if (set == null) set = new CopyOnWriteArraySet<>();
            boolean wasEmpty = set.isEmpty();
            set.add(sessionId);
            if (wasEmpty) {
                broadcastPresence(roomCode, true);
            }
            return set;
        });
    }

    public void onDisconnect(String sessionId, Long accountId) {
        // 세션 하나만 원자적으로 제거하고, Set이 비었는지 확인
        boolean[] becameEmpty = {false};
        sessions.computeIfPresent(accountId, (id, set) -> {
            set.remove(sessionId);
            becameEmpty[0] = set.isEmpty();
            return set;
        });

        // 세션이 아직 남아있으면 오프라인 처리 불필요 — 다른 세션이 살아있음
        if (!becameEmpty[0]) return;

        String roomCode = accountRooms.get(accountId);
        if (roomCode == null) return;

        taskScheduler.schedule(() -> {
            // 지연 후 재확인 (지연 시간 내 재연결 시 오프라인 브로드캐스트 취소)
            Set<String> remaining = sessions.get(accountId);
            if (remaining == null || remaining.isEmpty()) {
                broadcastPresence(roomCode, false);
            }
        }, Instant.now().plusMillis(offlineDelayMs));
    }

    public boolean isOnline(Long accountId) {
        Set<String> accountSessions = sessions.get(accountId);
        return accountSessions != null && !accountSessions.isEmpty();
    }

    // ── 뷰어 카운트 ─────────────────────────────────────────────────────────────

    /**
     * SUBSCRIBE 시 호출. 호스트 본인(accountRooms[accountId] == roomCode)은 카운트에서 제외.
     *
     * @param accountId null이면 익명 게스트
     */
    public void addViewer(String sessionId, String roomCode, Long accountId) {
        // 역인덱스 등록 (DISCONNECT 정리용)
        sessionRooms.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(roomCode);

        // 호스트 본인이면 뷰어 카운트 제외
        if (accountId != null && roomCode.equals(accountRooms.get(accountId))) {
            return;
        }

        CopyOnWriteArraySet<String> viewers =
                viewerSessions.computeIfAbsent(roomCode, k -> new CopyOnWriteArraySet<>());
        int before = viewers.size();
        viewers.add(sessionId);
        int after = viewers.size();
        if (after != before) {
            broadcastViewerCount(roomCode, after);
        }
    }

    /**
     * 세션 DISCONNECT 시 항상 호출 (호스트·게스트 무관).
     * 해당 세션이 구독하던 모든 방의 뷰어 집합에서 제거.
     */
    public void removeViewerSessions(String sessionId) {
        Set<String> rooms = sessionRooms.remove(sessionId);
        if (rooms == null) return;
        for (String roomCode : rooms) {
            CopyOnWriteArraySet<String> viewers = viewerSessions.get(roomCode);
            if (viewers == null) continue;
            int before = viewers.size();
            viewers.remove(sessionId);
            int after = viewers.size();
            if (after != before) {
                broadcastViewerCount(roomCode, after);
            }
        }
    }

    public int getViewerCount(String roomCode) {
        Set<String> viewers = viewerSessions.get(roomCode);
        return viewers == null ? 0 : viewers.size();
    }

    // ── 브로드캐스트 헬퍼 ───────────────────────────────────────────────────────

    private void broadcastPresence(String roomCode, boolean online) {
        messagingTemplate.convertAndSend("/topic/rooms/" + roomCode,
                new PresenceChangedEvent(online));
    }

    private void broadcastViewerCount(String roomCode, int count) {
        messagingTemplate.convertAndSend("/topic/rooms/" + roomCode,
                new ViewerCountChangedEvent(count));
    }
}
