package com.careflow.ai.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {

    Optional<IdempotencyKey> findByIdemKey(String idemKey);

    /** TTL housekeeping: the scheduled purger removes expired replay records. */
    @Modifying
    @Query("DELETE FROM IdempotencyKey k WHERE k.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
