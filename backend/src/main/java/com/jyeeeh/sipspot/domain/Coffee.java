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

    @Column(length = 200)
    private String message;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Coffee() {}

    public Coffee(Room room, String message) {
        this.room = room;
        this.message = message;
        this.status = "FREE_SENT";
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Room getRoom() { return room; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
