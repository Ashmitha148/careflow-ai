package com.careflow.ai.repository;

import com.careflow.ai.entity.EventType;
import com.careflow.ai.entity.TimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface TimelineEventRepository extends JpaRepository<TimelineEvent, UUID> {

    List<TimelineEvent> findByPatient_IdOrderByCreatedAtAsc(UUID patientId);

    List<TimelineEvent> findByPatient_IdAndEventTypeOrderByCreatedAtAsc(UUID patientId, EventType eventType);

    List<TimelineEvent> findByPatient_IdAndCreatedAtBetweenOrderByCreatedAtAsc(
            UUID patientId, LocalDateTime start, LocalDateTime end);

    List<TimelineEvent> findByPatient_IdAndEventTypeAndCreatedAtBetweenOrderByCreatedAtAsc(
            UUID patientId, EventType eventType, LocalDateTime start, LocalDateTime end);

    List<TimelineEvent> findByPatient_IdInOrderByCreatedAtDesc(Collection<UUID> patientIds);

    @Query("""
            SELECT e FROM TimelineEvent e
            WHERE e.patient.id = :patientId
              AND (:eventType IS NULL OR e.eventType = :eventType)
              AND (:start IS NULL OR e.createdAt >= :start)
              AND (:end IS NULL OR e.createdAt <= :end)
            ORDER BY e.createdAt ASC
            """)
    List<TimelineEvent> findFiltered(
            @Param("patientId") UUID patientId,
            @Param("eventType") EventType eventType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
