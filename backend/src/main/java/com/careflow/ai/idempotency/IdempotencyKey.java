package com.careflow.ai.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Replay record for a mutating clinical request carrying an Idempotency-Key
 * header (see V6__outbox_idempotency_async_ai.sql).
 *
 * A retried request with the same key replays the stored response instead of
 * executing again — a flaky mobile connection can therefore never double-write
 * a medication administration or diagnosis. Rows expire after a TTL and are
 * purged by {@link IdempotencyKeyFilter}.
 */
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {

    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_COMPLETED = "COMPLETED";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The client-supplied Idempotency-Key header value (unique). */
    @Column(name = "idem_key", nullable = false, length = 128)
    private String idemKey;

    /** Method + path the key was first used on (for misuse diagnostics). */
    @Column(name = "endpoint", nullable = false, length = 255)
    private String endpoint;

    /** SHA-256 of method|path — rejects key reuse across different endpoints. */
    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "status", nullable = false, length = 16)
    private String status = STATUS_PROCESSING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public IdempotencyKey() {}

    public UUID getId() { return id; }
    public String getIdemKey() { return idemKey; }
    public void setIdemKey(String idemKey) { this.idemKey = idemKey; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getRequestFingerprint() { return requestFingerprint; }
    public void setRequestFingerprint(String requestFingerprint) { this.requestFingerprint = requestFingerprint; }
    public Integer getResponseStatus() { return responseStatus; }
    public void setResponseStatus(Integer responseStatus) { this.responseStatus = responseStatus; }
    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
