package com.jyeeeh.sipspot.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "max_members", nullable = false)
    private int maxMembers = 8;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Member> members = new ArrayList<>();

    protected Room() {}

    public Room(String code) {
        this.code = code;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public int getMaxMembers() { return maxMembers; }
    public List<Member> getMembers() { return members; }
}
