package com.careflow.ai.service;

import com.careflow.ai.entity.EventType;
import com.careflow.ai.entity.Notification;
import com.careflow.ai.entity.NotificationType;
import com.careflow.ai.entity.Patient;
import com.careflow.ai.entity.User;
import com.careflow.ai.entity.Vital;
import com.careflow.ai.entity.VitalType;
import com.careflow.ai.repository.NotificationRepository;
import com.careflow.ai.repository.PatientRepository;
import com.careflow.ai.repository.UserRepository;
import com.careflow.ai.repository.VitalRepository;
import com.careflow.ai.entity.VitalThreshold;
import com.careflow.ai.dto.CursorPageResponse;
import com.careflow.ai.security.PatientAuthorizationService;
import com.careflow.ai.websocket.RealtimeEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class VitalService {

    private final VitalRepository vitalRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final PatientAuthorizationService patientAuthorizationService;
    private final TimelineService timelineService;
    private final AuditLogService auditLogService;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final VitalThresholdService vitalThresholdService;

    public VitalService(
            VitalRepository vitalRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            NotificationRepository notificationRepository,
            PatientAuthorizationService patientAuthorizationService,
            TimelineService timelineService,
            AuditLogService auditLogService,
            RealtimeEventPublisher realtimeEventPublisher,
            VitalThresholdService vitalThresholdService) {
        this.vitalRepository = vitalRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.patientAuthorizationService = patientAuthorizationService;
        this.timelineService = timelineService;
        this.auditLogService = auditLogService;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.vitalThresholdService = vitalThresholdService;
    }

    @Transactional
    public Vital recordVital(UUID patientId, Vital vital) {

        patientAuthorizationService.ensurePatientAccess(patientId);

        User recordedBy = resolveCurrentUser();

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found: " + patientId));

        vital.setPatient(patient);
        vital.setRecordedBy(recordedBy);

        if (vital.getRecordedAt() == null) {
            vital.setRecordedAt(LocalDateTime.now());
        }

        Vital savedVital = vitalRepository.save(vital);

        // Live vitals stream — same entity shape GET /clinical/vitals/{patientId} returns.
        realtimeEventPublisher.publishVital(patientId, savedVital);

        boolean critical = isCritical(savedVital);

        String description = "Vital recorded: "
                + savedVital.getType()
                + " = "
                + savedVital.getValue();

        if (critical) {
            description += " [CRITICAL]";

            timelineService.appendEvent(
                    patientId,
                    EventType.CRITICAL_ALERT,
                    description,
                    recordedBy.getId()
            );

            createCriticalNotification(
                    patient.getAssignedDoctor(),
                    description
            );

            createCriticalNotification(
                    patient.getAssignedNurse(),
                    description
            );

        } else {

            timelineService.appendEvent(
                    patientId,
                    EventType.VITAL,
                    description,
                    recordedBy.getId()
            );
        }

        auditLogService.log(
                recordedBy.getId(),
                "RECORD_VITAL",
                "Vital",
                savedVital.getId(),
                description
        );

        return savedVital;
    }

    private User resolveCurrentUser() {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof com.careflow.ai.security.CustomUserDetails details)) {
            throw new RuntimeException("Authentication required");
        }
        return details.getUser();
    }

    public boolean isCritical(Vital vital) {

        if (vital == null || vital.getType() == null || vital.getValue() == null) {
            return false;
        }

        // Per-patient override wins (Phase 5); fall back to the global
        // clinical defaults when the patient has no configured threshold.
        if (vital.getPatient() != null) {
            VitalThreshold override = vitalThresholdService
                    .findOverride(vital.getPatient().getId(), vital.getType())
                    .orElse(null);
            if (override != null) {
                return vitalThresholdService.isCriticalWithOverride(vital, override);
            }
        }

        String value = vital.getValue().trim();

        try {
            switch (vital.getType()) {

                case OXYGEN:
                    return Double.parseDouble(value) < 90.0;

                case BLOOD_PRESSURE:
                    String[] parts = value.split("[/\\-]");

                    if (parts.length != 2) {
                        return false;
                    }

                    double systolic = Double.parseDouble(parts[0].trim());
                    double diastolic = Double.parseDouble(parts[1].trim());

                    return systolic > 180.0 || diastolic > 120.0;

                case TEMPERATURE:
                    return isCriticalTemperature(value);

                default:
                    return false;
            }

        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Human-readable description of the configured critical threshold for a
     * vital type. Mirrors the rules in {@link #isCritical(Vital)} so the UI can
     * explain exactly why an alert fired, without changing detection behavior.
     */
    public static String describeThreshold(VitalType type) {
        if (type == null) {
            return "N/A";
        }
        switch (type) {
            case OXYGEN:
                return "Oxygen saturation < 90%";
            case BLOOD_PRESSURE:
                return "Systolic > 180 mmHg or diastolic > 120 mmHg";
            case TEMPERATURE:
                return "Temperature > 39.4 \u00B0C (103 \u00B0F)";
            default:
                return "N/A";
        }
    }
    private boolean isCriticalTemperature(String value) {

        String normalized = value.toUpperCase();

        if (normalized.endsWith("C")) {
            String numericValue =
                    normalized.substring(0, normalized.length() - 1).trim();

            return Double.parseDouble(numericValue) > 39.4;
        }

        if (normalized.endsWith("F")) {
            String numericValue =
                    normalized.substring(0, normalized.length() - 1).trim();

            return Double.parseDouble(numericValue) > 103.0;
        }

        /*
         * Existing Vital.value is a String without a separate unit field.
         * For an unqualified temperature value, the project uses Fahrenheit.
         */
        return Double.parseDouble(value) > 103.0;
    }

    private void createCriticalNotification(User user, String message) {

        if (user == null) {
            return;
        }

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(NotificationType.CRITICAL_VITAL);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification);

        realtimeEventPublisher.publishNotification(user.getId(), notification);
    }

    @Transactional(readOnly = true)
    public java.util.List<Vital> getPatientVitals(UUID patientId) {

        patientAuthorizationService.ensurePatientAccess(patientId);

        if (!patientRepository.existsById(patientId)) {
            throw new RuntimeException("Patient not found: " + patientId);
        }

        return vitalRepository.findByPatient_IdOrderByRecordedAtDesc(patientId);
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<Vital> getPatientVitalsCursor(UUID patientId, String cursor, int limit) {
        patientAuthorizationService.ensurePatientAccess(patientId);

        if (!patientRepository.existsById(patientId)) {
            throw new RuntimeException("Patient not found: " + patientId);
        }
        int pageSize = Math.min(Math.max(limit, 1), 100);

        List<Vital> rawVitals;
        if (cursor != null && !cursor.isBlank()) {
            try {
                UUID cursorId = UUID.fromString(cursor.trim());
                Vital cursorVital = vitalRepository.findById(cursorId).orElse(null);
                if (cursorVital != null) {
                    rawVitals = vitalRepository.findByPatient_IdAndRecordedAtLessThanOrderByRecordedAtDesc(
                            patientId, cursorVital.getRecordedAt(), org.springframework.data.domain.PageRequest.of(0, pageSize + 1));
                } else {
                    rawVitals = vitalRepository.findByPatient_IdOrderByRecordedAtDesc(
                            patientId, org.springframework.data.domain.PageRequest.of(0, pageSize + 1));
                }
            } catch (IllegalArgumentException e) {
                rawVitals = vitalRepository.findByPatient_IdOrderByRecordedAtDesc(
                        patientId, org.springframework.data.domain.PageRequest.of(0, pageSize + 1));
            }
        } else {
            rawVitals = vitalRepository.findByPatient_IdOrderByRecordedAtDesc(
                    patientId, org.springframework.data.domain.PageRequest.of(0, pageSize + 1));
        }

        boolean hasMore = rawVitals.size() > pageSize;
        java.util.List<Vital> items = hasMore ? rawVitals.subList(0, pageSize) : rawVitals;
        String nextCursor = (hasMore && !items.isEmpty()) ? items.get(items.size() - 1).getId().toString() : null;

        return CursorPageResponse.<Vital>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .pageSize(pageSize)
                .build();
    }

    @Transactional(readOnly = true)
    public java.util.List<Vital> getPatientVitalsByType(
            UUID patientId,
            VitalType type) {

        patientAuthorizationService.ensurePatientAccess(patientId);

        if (!patientRepository.existsById(patientId)) {
            throw new RuntimeException("Patient not found: " + patientId);
        }

        return vitalRepository
                .findByPatient_IdAndTypeOrderByRecordedAtDesc(patientId, type);
    }
}
