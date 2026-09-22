package com.jyeeeh.sipspot.service;

import com.jyeeeh.sipspot.domain.Location;
import com.jyeeeh.sipspot.domain.Room;
import com.jyeeeh.sipspot.dto.ws.LocationChangedEvent;
import com.jyeeeh.sipspot.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);

    private final RoomRepository roomRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public LocationService(RoomRepository roomRepository,
                           SimpMessagingTemplate messagingTemplate) {
        this.roomRepository = roomRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void updateLocation(Long accountId, String roomCode, Location location) {
        Room room = roomRepository.findByCodeWithHost(roomCode).orElse(null);
        if (room == null) return;

        // 방어적 소유권 검증 (LocationMessageController에서 이미 확인하지만 이중 방어)
        if (!room.getHost().getId().equals(accountId)) {
            log.warn("위치 변경 거부: accountId={} != room.hostId={}", accountId, room.getHost().getId());
            return;
        }

        room.updateLocation(location);
        roomRepository.save(room);

        messagingTemplate.convertAndSend("/topic/rooms/" + roomCode,
                new LocationChangedEvent(location.name()));
    }
}
