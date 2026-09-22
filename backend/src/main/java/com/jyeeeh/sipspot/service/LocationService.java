package com.jyeeeh.sipspot.service;

import com.jyeeeh.sipspot.domain.Location;
import com.jyeeeh.sipspot.domain.Member;
import com.jyeeeh.sipspot.dto.ws.LocationChangedEvent;
import com.jyeeeh.sipspot.repository.MemberRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class LocationService {

    private final MemberRepository memberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public LocationService(MemberRepository memberRepository,
                           SimpMessagingTemplate messagingTemplate) {
        this.memberRepository = memberRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void updateLocation(UUID memberId, String roomCode, Location location) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("멤버를 찾을 수 없습니다: " + memberId));

        member.updateLocation(location);
        memberRepository.save(member);

        // 민감 필드(tokenHash 등) 없이 브로드캐스트
        LocationChangedEvent event = new LocationChangedEvent(
                member.getId(), member.getNickname(), location.name()
        );
        messagingTemplate.convertAndSend("/topic/rooms/" + roomCode, event);
    }
}
