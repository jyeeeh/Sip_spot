package com.jyeeeh.sipspot.service;

import com.jyeeeh.sipspot.dto.ws.PresenceChangedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class PresenceRegistry {

    private static final long OFFLINE_DELAY_MS = 3_000L;

    private final ConcurrentHashMap<UUID, CopyOnWriteArraySet<String>> sessions =
            new ConcurrentHashMap<>();
    // memberId → roomCode (방 코드 브로드캐스트에 필요)
    private final ConcurrentHashMap<UUID, String> memberRooms = new ConcurrentHashMap<>();

    private final SimpMessagingTemplate messagingTemplate;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final long offlineDelayMs;

    // 프로덕션용 생성자
    @Autowired
    public PresenceRegistry(SimpMessagingTemplate messagingTemplate,
                            ThreadPoolTaskScheduler taskScheduler) {
        this(messagingTemplate, taskScheduler, OFFLINE_DELAY_MS);
    }

    // 테스트용 생성자 (지연 시간 주입 가능)
    public PresenceRegistry(SimpMessagingTemplate messagingTemplate,
                            ThreadPoolTaskScheduler taskScheduler,
                            long offlineDelayMs) {
        this.messagingTemplate = messagingTemplate;
        this.taskScheduler = taskScheduler;
        this.offlineDelayMs = offlineDelayMs;
    }

    public void onConnect(String sessionId, UUID memberId, String roomCode) {
        memberRooms.put(memberId, roomCode);
        sessions.compute(memberId, (id, set) -> {
            if (set == null) set = new CopyOnWriteArraySet<>();
            boolean wasEmpty = set.isEmpty();
            set.add(sessionId);
            if (wasEmpty) {
                broadcast(memberId, roomCode, true);
            }
            return set;
        });
    }

    public void onDisconnect(String sessionId, UUID memberId) {
        Set<String> memberSessions = sessions.get(memberId);
        if (memberSessions != null) {
            memberSessions.remove(sessionId);
        }
        // 오프라인 처리를 지연해서 새로고침 깜빡임 방지
        String roomCode = memberRooms.get(memberId);
        if (roomCode == null) return;

        taskScheduler.schedule(() -> {
            Set<String> remaining = sessions.get(memberId);
            if (remaining == null || remaining.isEmpty()) {
                broadcast(memberId, roomCode, false);
            }
        }, Instant.now().plusMillis(offlineDelayMs));
    }

    public boolean isOnline(UUID memberId) {
        Set<String> memberSessions = sessions.get(memberId);
        return memberSessions != null && !memberSessions.isEmpty();
    }

    private void broadcast(UUID memberId, String roomCode, boolean online) {
        PresenceChangedEvent event = new PresenceChangedEvent(memberId, online);
        messagingTemplate.convertAndSend("/topic/rooms/" + roomCode, event);
    }
}
