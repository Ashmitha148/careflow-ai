package com.careflow.ai.service;

import com.careflow.ai.dto.CursorPageResponse;
import com.careflow.ai.dto.TimelineDto.CriticalAlertDetail;
import com.careflow.ai.dto.TimelineDto.NotifiedUser;
import com.careflow.ai.dto.TimelineDto.TimelineEventResponse;
import com.careflow.ai.entity.EventType;
import com.careflow.ai.entity.Notification;
import com.careflow.ai.entity.NotificationType;
import com.careflow.ai.entity.Patient;
import com.careflow.ai.entity.TimelineEvent;
import com.careflow.ai.entity.User;
import com.careflow.ai.entity.VitalType;
import com.careflow.ai.repository.NotificationRepository;
import com.careflow.ai.repository.PatientAccessRepository;
import com.careflow.ai.repository.PatientRepository;
import com.careflow.ai.repository.TimelineEventRepository;
import com.careflow.ai.repository.UserRepository;
import com.careflow.ai.security.PatientAuthorizationService;
import com.careflow.ai.websocket.RealtimeEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TimelineService {

    private final TimelineEventRepository timelineEventRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final PatientAccessRepository patientAccessRepository;
    private final PatientAuthorizationService patientAuthorizationService;
    private final AuditLogService auditLogService;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final VitalThresholdService vitalThresholdService;

    public TimelineService(TimelineEventRepository timelineEventRepository,
                           PatientRepository patientRepository,
                           UserRepository userRepository,
                           NotificationRepository notificationRepository,
                           PatientAccessRepository patientAccessRepository,
                           PatientAuthorizationService patientAuthorizationService,
                           AuditLogService auditLogService,
                           RealtimeEventPublisher realtimeEventPublisher,
                           VitalThresholdService vitalThresholdService) {
        this.timelineEventRepository = timelineEventRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.patientAccessRepository = patientAccessRepository;
        this.patientAuthorizationService = patientAuthorizationService;
        this.auditLogService = auditLogService;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.vitalThresholdService = vitalThresholdService;
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
                                              String correctionDescription) {
        TimelineEvent original = timelineEventRepository.findById(correctsEventId)
                .orElseThrow(() -> new RuntimeException("Timeline event not found: " + correctsEventId));

        User actor = resolveCurrentUser();

        return appendEvent(
                original.getPatient().getId(),
                original.getEventType(),
                correctionDescription,
                actor.getId(),
                original
        );
    }

    private User resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof com.careflow.ai.security.CustomUserDetails details)) {
            throw new RuntimeException("Authentication required");
        }
        return details.getUser();
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<TimelineEventResponse> getTimelineCursor(UUID patientId,
                                                                         EventType eventType,
                                                                         LocalDateTime start,
                                                                         LocalDateTime end,
                                                                         String cursor,
                                                                         int limit) {
        ensurePatientExists(patientId);
        patientAuthorizationService.ensurePatientAccess(patientId);

        int pageSize = Math.min(Math.max(limit, 1), 100);

        org.springframework.data.jpa.domain.Specification<TimelineEvent> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("patient").get("id"), patientId));
            if (eventType != null) {
                predicates.add(cb.equal(root.get("eventType"), eventType));
            }
            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
            }
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
            }
            if (cursor != null && !cursor.isBlank()) {
                try {
                    UUID cursorId = UUID.fromString(cursor.trim());
                    TimelineEvent cursorEvent = timelineEventRepository.findById(cursorId).orElse(null);
                    if (cursorEvent != null) {
                        predicates.add(cb.or(
                                cb.greaterThan(root.get("createdAt"), cursorEvent.getCreatedAt()),
                                cb.and(
                                        cb.equal(root.get("createdAt"), cursorEvent.getCreatedAt()),
                                        cb.greaterThan(root.get("id"), cursorId)
                                )
                        ));
                    }
                } catch (IllegalArgumentException e) {
                    // Ignore invalid cursor format gracefully
                }
            }
            query.orderBy(cb.asc(root.get("createdAt")), cb.asc(root.get("id")));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        List<TimelineEvent> rawEvents = timelineEventRepository.findAll(spec, org.springframework.data.domain.PageRequest.of(0, pageSize + 1)).getContent();
        boolean hasMore = rawEvents.size() > pageSize;
        List<TimelineEvent> pageEvents = hasMore ? rawEvents.subList(0, pageSize) : rawEvents;

        List<TimelineEventResponse> responses = pageEvents.stream().map(this::toResponse).toList();
        String nextCursor = (hasMore && !responses.isEmpty()) ? responses.get(responses.size() - 1).getId().toString() : null;

        return CursorPageResponse.<TimelineEventResponse>builder()
                .items(responses)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .pageSize(pageSize)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TimelineEventResponse> getTimeline(UUID patientId,
                                                   EventType eventType,
                                                   LocalDateTime start,
                                                   LocalDateTime end) {
        return getTimelineCursor(patientId, eventType, start, end, null, 100).getItems();
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

        TimelineEventResponse response = toResponse(event);

        // Realtime fan-out. The same DTO returned by GET /patients/{id}/timeline
        // is published to /topic/patient/{patientId}/timeline so clients can
        // apply it with queryClient.setQueryData — no refetch, and no drift
        // between what REST returns and what the socket delivers.
        realtimeEventPublisher.publishTimelineEvent(event.getPatient().getId(), response);

        return response;
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
                .criticalAlert(event.getEventType() == EventType.CRITICAL_ALERT
                        ? buildCriticalAlertDetail(event)
                        : null)
                .build();
    }

    /**
     * Builds the "why was this alert triggered?" detail from real stored data on
     * the alert event itself and the notifications that were actually created.
     */
    private CriticalAlertDetail buildCriticalAlertDetail(TimelineEvent event) {
        String description = event.getDescription();
        VitalType vitalType = parseVitalType(description);
        String recordedValue = parseRecordedValue(description);

        User recordedBy = event.getCreatedBy();
        User doctor = event.getPatient().getAssignedDoctor();
        User nurse = event.getPatient().getAssignedNurse();

        Set<UUID> notifiedIds = new HashSet<>();
        if (doctor != null) {
            notifiedIds.add(doctor.getId());
        }
        if (nurse != null) {
            notifiedIds.add(nurse.getId());
        }

        LocalDateTime createdAt = event.getCreatedAt();
        LocalDateTime from = createdAt != null ? createdAt : LocalDateTime.now().minusMinutes(1);
        LocalDateTime to = createdAt != null ? createdAt.plusMinutes(1) : LocalDateTime.now();

        List<Notification> notifications = notificationRepository
                .findByTypeAndMessageAndCreatedAtBetween(
                        NotificationType.CRITICAL_VITAL,
                        description,
                        from,
                        to);

        LinkedHashSet<NotifiedUser> notifiedUsers = new LinkedHashSet<>();
        for (Notification notification : notifications) {
            User recipient = notification.getUser();
            if (recipient != null && notifiedIds.contains(recipient.getId())) {
                notifiedUsers.add(NotifiedUser.builder()
                        .id(recipient.getId())
                        .fullName(recipient.getFullName())
                        .build());
            }
        }

        return CriticalAlertDetail.builder()
                .vitalType(vitalType != null ? vitalType.name() : null)
                .recordedValue(recordedValue)
                .threshold(vitalThresholdService.describeActiveThreshold(
                        event.getPatient().getId(), vitalType))
                .timestamp(createdAt)
                .recordedById(recordedBy != null ? recordedBy.getId() : null)
                .recordedByName(recordedBy != null ? recordedBy.getFullName() : null)
                .notifiedUsers(new ArrayList<>(notifiedUsers))
                .build();
    }

    private VitalType parseVitalType(String description) {
        String token = parseVitalToken(description);
        if (token == null) {
            return null;
        }
        int sep = token.indexOf(" = ");
        String typeName = sep > 0 ? token.substring(0, sep).trim() : token.trim();
        for (VitalType type : VitalType.values()) {
            if (type.name().equalsIgnoreCase(typeName)) {
                return type;
            }
        }
        return null;
    }

    private String parseRecordedValue(String description) {
        String token = parseVitalToken(description);
        if (token == null) {
            return null;
        }
        int sep = token.indexOf(" = ");
        return sep >= 0 ? token.substring(sep + 3).trim() : null;
    }

    private String parseVitalToken(String description) {
        if (description == null) {
            return null;
        }
        String text = description;
        if (text.endsWith(" [CRITICAL]")) {
            text = text.substring(0, text.length() - " [CRITICAL]".length());
        }
        String prefix = "Vital recorded: ";
        if (!text.startsWith(prefix)) {
            return null;
        }
        return text.substring(prefix.length()).trim();
    }

}
