package com.jyeeeh.sipspot.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "member")
public class Member {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false, length = 12)
    private String nickname;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "is_host", nullable = false)
    private boolean host;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private OffsetDateTime joinedAt;

    protected Member() {}

    public Member(Room room, String nickname, String tokenHash, boolean host) {
        this.id = UUID.randomUUID();
        this.room = room;
        this.nickname = nickname;
        this.tokenHash = tokenHash;
        this.host = host;
        this.joinedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public Room getRoom() { return room; }
    public String getNickname() { return nickname; }
    public String getTokenHash() { return tokenHash; }
    public boolean isHost() { return host; }
    public OffsetDateTime getJoinedAt() { return joinedAt; }
}
