package com.jyeeeh.sipspot.dto;

import com.jyeeeh.sipspot.domain.Member;
import com.jyeeeh.sipspot.domain.Room;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public record RoomDetailResponse(
        String code,
        int maxMembers,
        OffsetDateTime createdAt,
        List<MemberInfo> members
) {
    public record MemberInfo(
            UUID id,
            String nickname,
            boolean host,
            OffsetDateTime joinedAt,
            String location,
            boolean online
    ) {}

    public static RoomDetailResponse from(Room room, List<Member> members,
                                          Function<UUID, Boolean> onlineCheck) {
        List<MemberInfo> memberInfos = members.stream()
                .map(m -> new MemberInfo(
                        m.getId(),
                        m.getNickname(),
                        m.isHost(),
                        m.getJoinedAt(),
                        m.getLocation().name(),
                        onlineCheck.apply(m.getId())
                ))
                .toList();
        return new RoomDetailResponse(room.getCode(), room.getMaxMembers(), room.getCreatedAt(), memberInfos);
    }
}
