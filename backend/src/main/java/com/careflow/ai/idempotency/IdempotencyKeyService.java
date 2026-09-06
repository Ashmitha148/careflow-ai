package com.careflow.ai.idempotency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Replay-record store for mutating clinical requests carrying an
 * Idempotency-Key header (see {@link IdempotencyKeyFilter} for the request
 * lifecycle). A retried request with the same key replays the stored response
 * instead of executing again — a flaky mobile connection can therefore never
 * double-write a medication administration or diagnosis.
 *
 * The unique constraint on idem_key is the concurrency gate: two simultaneous
 * requests with the same key cannot both insert, so exactly one executes and
 * the other is told the key is in flight.
 */
@Service
public class IdempotencyKeyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyKeyService.class);

    private final IdempotencyKeyRepository repository;
    private final long ttlHours;

    public IdempotencyKeyService(
            IdempotencyKeyRepository repository,
            @Value("${careflow.idempotency.ttl-hours:24}") long ttlHours) {
        this.repository = repository;
        this.ttlHours = ttlHours;
    }

    /** The stored record, unless it has outlived its TTL. */
    public Optional<IdempotencyKey> findActive(String idemKey) {
        return repository.findByIdemKey(idemKey)
                .filter(record -> record.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    /**
     * Registers the key as PROCESSING. Throws
     * {@link IdempotencyKeyInFlightException} if a concurrent request already
     * holds the key (unique-constraint violation) — the filter maps that to
     * HTTP 409 rather than executing twice.
     */
    @Transactional
    public IdempotencyKey start(String idemKey, String endpoint, String fingerprint) {
        IdempotencyKey record = new IdempotencyKey();
        record.setIdemKey(idemKey);
        record.setEndpoint(endpoint);
        record.setRequestFingerprint(fingerprint);
        record.setStatus(IdempotencyKey.STATUS_PROCESSING);
        record.setExpiresAt(LocalDateTime.now().plusHours(ttlHours));
        try {
            // flush() surfaces the unique violation HERE, inside the catch,
            // instead of at commit time when it is too late to map to a 409.
            return repository.saveAndFlush(record);
        } catch (DataIntegrityViolationException e) {
            throw new IdempotencyKeyInFlightException(idemKey);
        }
    }

    /** Stores the response for replay. The row survives process restarts. */
    @Transactional
    public void complete(String idemKey, int responseStatus, String responseBody) {
        repository.findByIdemKey(idemKey).ifPresent(record -> {
            record.setResponseStatus(responseStatus);
            record.setResponseBody(responseBody);
            record.setStatus(IdempotencyKey.STATUS_COMPLETED);
        });
    }

    /**
     * Drops a PROCESSING row after a 5xx so the client's retry re-executes
     * instead of replaying a server failure.
     */
    @Transactional
    public void abandon(String idemKey) {
        repository.findByIdemKey(idemKey).ifPresent(repository::delete);
    }

    /** TTL housekeeping: expired replay records are purged every 15 minutes. */
    @Scheduled(fixedDelay = 900_000)
    @Transactional
    public void purgeExpired() {
        int removed = repository.deleteExpired(LocalDateTime.now());
        if (removed > 0) {
            log.info("Purged {} expired idempotency key record(s)", removed);
        }
    }
}