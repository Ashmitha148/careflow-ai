package com.careflow.ai.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Atomically claims a batch of due rows by flipping them to RELAYED in one
     * statement. The subquery+UPDATE runs in the relay's transaction, so two
     * relay instances (or a restart mid-flight) can never double-claim: the
     * second claimer's UPDATE sees the already-RELAYED rows and skips them.
     * Returns the claimed rows for processing.
     */
    @Modifying
    @Query(value = """
            UPDATE outbox_events
               SET status = 'RELAYED',
                   attempts = attempts + 1
             WHERE id IN (
                   SELECT id FROM outbox_events
                    WHERE status = 'PENDING'
                      AND next_attempt_at <= :now
                    ORDER BY created_at
                    LIMIT :batchSize
             )
            """, nativeQuery = true)
    int claimDueBatch(@Param("now") LocalDateTime now, @Param("batchSize") int batchSize);

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status);

    /** Forensic/reporting aid: how many events are still awaiting delivery. */
    long countByStatus(OutboxStatus status);
}
