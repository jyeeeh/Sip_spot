package com.jyeeeh.sipspot.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "account_session")
public class AccountSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected AccountSession() {}

    public AccountSession(Account account, String tokenHash) {
        this.account = account;
        this.tokenHash = tokenHash;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Account getAccount() { return account; }
    public String getTokenHash() { return tokenHash; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
