package com.careflow.ai.service;

import com.careflow.ai.dto.TimelineDto.TimelineEventResponse;
import com.careflow.ai.entity.EventType;
import com.careflow.ai.entity.Patient;
import com.careflow.ai.entity.TimelineEvent;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.PatientRepository;
import com.careflow.ai.repository.TimelineEventRepository;
import com.careflow.ai.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TimelineService {

    private final TimelineEventRepository timelineEventRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public TimelineService(TimelineEventRepository timelineEventRepository,
                           PatientRepository patientRepository,
                           UserRepository userRepository,
                           AuditLogService auditLogService) {
        this.timelineEventRepository = timelineEventRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Append-only write. Existing timeline rows are never updated or deleted.
     */
    @Transactional
    public TimelineEventResponse appendEvent(UUID patientId,
                                             EventType eventType,
                                             String description,
                                             UUID createdByUserId) {
        return appendEvent(patientId, eventType, description, createdByUserId, null);
    }

    @Transactional
    public TimelineEventResponse correctEvent(UUID correctsEventId,
                                              String correctionDescription,
                                              UUID createdByUserId) {
        TimelineEvent original = timelineEventRepository.findById(correctsEventId)
                .orElseThrow(() -> new RuntimeException("Timeline event not found: " + correctsEventId));

        return appendEvent(
                original.getPatient().getId(),
                original.getEventType(),
                correctionDescription,
                createdByUserId,
                original
        );
    }

    @Transactional(readOnly = true)
    public List<TimelineEventResponse> getTimeline(UUID patientId,
                                                   EventType eventType,
                                                   LocalDateTime start,
                                                   LocalDateTime end) {
        ensurePatientExists(patientId);
        return timelineEventRepository.findFiltered(patientId, eventType, start, end).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TimelineEventResponse> replayLast24Hours(UUID patientId) {
        LocalDateTime end = LocalDateTime.now();
        return getTimeline(patientId, null, end.minusHours(24), end);
    }

    @Transactional(readOnly = true)
    public List<TimelineEventResponse> replayLast7Days(UUID patientId) {
        LocalDateTime end = LocalDateTime.now();
        return getTimeline(patientId, null, end.minusDays(7), end);
    }

    private TimelineEventResponse appendEvent(UUID patientId,
                                              EventType eventType,
                                              String description,
                                              UUID createdByUserId,
                                              TimelineEvent correctsEvent) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found: " + patientId));

        User createdBy = null;
        if (createdByUserId != null) {
            createdBy = userRepository.findById(createdByUserId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + createdByUserId));
        }

        TimelineEvent event = TimelineEvent.builder()
                .patient(patient)
                .eventType(eventType)
                .description(description)
                .correctsEvent(correctsEvent)
                .createdBy(createdBy)
                .build();

        event = timelineEventRepository.save(event);

        String action = correctsEvent != null ? "TIMELINE_EVENT_CORRECTED" : "TIMELINE_EVENT_APPENDED";
        String metadata = "eventType=" + eventType
                + (correctsEvent != null ? ", correctsEventId=" + correctsEvent.getId() : "");

        auditLogService.log(createdByUserId, action, "TimelineEvent", event.getId(), metadata);

        return toResponse(event);
    }

    private void ensurePatientExists(UUID patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new RuntimeException("Patient not found: " + patientId);
        }
    }

    private TimelineEventResponse toResponse(TimelineEvent event) {
        User createdBy = event.getCreatedBy();
        return TimelineEventResponse.builder()
                .id(event.getId())
                .patientId(event.getPatient().getId())
                .eventType(event.getEventType())
                .description(event.getDescription())
                .correctsEventId(event.getCorrectsEventId())
                .createdById(createdBy != null ? createdBy.getId() : null)
                .createdByName(createdBy != null ? createdBy.getFullName() : null)
                .createdAt(event.getCreatedAt())
                .build();
    }
}
