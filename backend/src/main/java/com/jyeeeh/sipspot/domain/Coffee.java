package com.jyeeeh.sipspot.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "coffee")
public class Coffee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "writer_name", nullable = false, length = 10)
    private String writerName;

    @Column(length = 200)
    private String message;

    @Column(nullable = false, length = 16)
    private String status;

    // 체크리스트 7: 비밀번호 해시는 이 엔티티 내부에만 존재하며 DTO에 노출하지 않는다
    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Coffee() {}

    public Coffee(Room room, String writerName, String message, String passwordHash) {
        this.room = room;
        this.writerName = writerName;
        this.message = message;
        this.passwordHash = passwordHash;
        this.status = "FREE_SENT";
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Room getRoom() { return room; }
    public String getWriterName() { return writerName; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
    public String getPasswordHash() { return passwordHash; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
