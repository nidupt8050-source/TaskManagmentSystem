package com.smarttask.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
    @Id @Column(length = 64)
    private String tokenHash;
    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    @Column(nullable = false)
    private Instant expiresAt;

    protected PasswordResetToken() {}
    public PasswordResetToken(String hash, User user, Instant expiry) {
        tokenHash = hash; this.user = user; expiresAt = expiry;
    }
    public String getTokenHash() { return tokenHash; }
    public User getUser() { return user; }
    public Instant getExpiresAt() { return expiresAt; }
}
