package com.careflow.ai.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * A refresh-token family represents one user/device login session. All refresh
 * tokens issued as part of a single login belong to the same family. If token
 * reuse is detected (a previously-used refresh token is replayed), the entire
 * family is revoked — this is the theft-detection mechanism.
 */
@Entity
@Table(name = "refresh_token_families")
public class RefreshTokenFamily {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "family_hash", nullable = false, length = 128)
    private String familyHash;

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getFamilyHash() { return familyHash; }
    public void setFamilyHash(String familyHash) { this.familyHash = familyHash; }

    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
}