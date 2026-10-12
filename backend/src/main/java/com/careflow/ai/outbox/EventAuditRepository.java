package com.careflow.ai.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link EventAudit} rows.
 *
 * <p>Only append operations are ever needed here — there are no update or delete
 * paths. The query methods support forensics and compliance reporting.
 */
public interface EventAuditRepository extends JpaRepository<EventAudit, UUID> {

    /** All audit rows for a given outbox event (re-delivery forensics). */
    List<EventAudit> findByOutboxId(UUID outboxId);

    /** All audit rows for a given clinical aggregate, e.g. all events for a patient timeline. */
    List<EventAudit> findByAggregateTypeAndAggregateId(String aggregateType, UUID aggregateId);
}
