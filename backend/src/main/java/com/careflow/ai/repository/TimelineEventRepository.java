package com.careflow.ai.repository;

import com.careflow.ai.entity.EventType;
import com.careflow.ai.entity.TimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface TimelineEventRepository extends JpaRepository<TimelineEvent, UUID>, JpaSpecificationExecutor<TimelineEvent> {

    List<TimelineEvent> findByPatient_IdOrderByCreatedAtAsc(UUID patientId);

    List<TimelineEvent> findByPatient_IdAndEventTypeOrderByCreatedAtAsc(UUID patientId, EventType eventType);

    List<TimelineEvent> findByPatient_IdAndCreatedAtBetweenOrderByCreatedAtAsc(
            UUID patientId, LocalDateTime start, LocalDateTime end);

    List<TimelineEvent> findByPatient_IdAndEventTypeAndCreatedAtBetweenOrderByCreatedAtAsc(
            UUID patientId, EventType eventType, LocalDateTime start, LocalDateTime end);

    List<TimelineEvent> findByPatient_IdInOrderByCreatedAtDesc(Collection<UUID> patientIds);
}
