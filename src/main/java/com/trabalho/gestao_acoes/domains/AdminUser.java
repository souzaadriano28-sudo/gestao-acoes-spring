package com.trabalho.gestao_acoes.domains;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "admin_user")
public class AdminUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "account_slot", nullable = false, unique = true)
    private short accountSlot = 1;
    @Column(nullable = false, unique = true, length = 64)
    private String username;
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    @Column(nullable = false)
    private boolean enabled = true;
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;
    @Column(name = "failure_window_started_at")
    private Instant failureWindowStartedAt;
    @Column(name = "locked_until")
    private Instant lockedUntil;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    protected AdminUser() {}
    public AdminUser(String username, String passwordHash, Instant now) {
        this.username = username; this.passwordHash = passwordHash; this.createdAt = now; this.updatedAt = now;
    }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isEnabled() { return enabled; }
    public int getFailedAttempts() { return failedAttempts; }
    public Instant getFailureWindowStartedAt() { return failureWindowStartedAt; }
    public Instant getLockedUntil() { return lockedUntil; }
    public long getVersion() { return version; }
    public boolean isLocked(Instant now) { return lockedUntil != null && lockedUntil.isAfter(now); }
    public void registerFailure(Instant now, int limit, java.time.Duration window, java.time.Duration lockDuration) {
        if (failureWindowStartedAt == null || !failureWindowStartedAt.plus(window).isAfter(now)) {
            failureWindowStartedAt = now; failedAttempts = 0;
        }
        failedAttempts++;
        if (failedAttempts >= limit) lockedUntil = now.plus(lockDuration);
        updatedAt = now;
    }
    public void clearFailures(Instant now) { failedAttempts = 0; failureWindowStartedAt = null; lockedUntil = null; updatedAt = now; }
}
