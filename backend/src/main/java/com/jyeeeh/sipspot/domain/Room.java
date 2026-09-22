package com.jyeeeh.sipspot.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "room")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 7)
    private String code;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_account_id", nullable = false)
    private Account host;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Location location;

    @Column(name = "location_updated_at", nullable = false)
    private OffsetDateTime locationUpdatedAt;

    protected Room() {}

    public Room(String code, Account host) {
        this.code = code;
        this.host = host;
        this.createdAt = OffsetDateTime.now();
        this.location = Location.LIVING_ROOM;
        this.locationUpdatedAt = OffsetDateTime.now();
    }

    public void updateLocation(Location location) {
        this.location = location;
        this.locationUpdatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public Account getHost() { return host; }
    public Location getLocation() { return location; }
    public OffsetDateTime getLocationUpdatedAt() { return locationUpdatedAt; }
}
