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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class VitalService {

    private final VitalRepository vitalRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final TimelineService timelineService;
    private final AuditLogService auditLogService;

    public VitalService(
            VitalRepository vitalRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            NotificationRepository notificationRepository,
            TimelineService timelineService,
            AuditLogService auditLogService) {
        this.vitalRepository = vitalRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.timelineService = timelineService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Vital recordVital(UUID patientId, UUID recordedById, Vital vital) {

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found: " + patientId));

        User recordedBy = userRepository.findById(recordedById)
                .orElseThrow(() -> new RuntimeException("User not found: " + recordedById));

        vital.setPatient(patient);
        vital.setRecordedBy(recordedBy);

        if (vital.getRecordedAt() == null) {
            vital.setRecordedAt(LocalDateTime.now());
        }

        Vital savedVital = vitalRepository.save(vital);

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
                    recordedById
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
                    recordedById
            );
        }

        auditLogService.log(
                recordedById,
                "RECORD_VITAL",
                "Vital",
                savedVital.getId(),
                description
        );

        return savedVital;
    }

    public boolean isCritical(Vital vital) {

        if (vital == null || vital.getType() == null || vital.getValue() == null) {
            return false;
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
    }

    @Transactional(readOnly = true)
    public java.util.List<Vital> getPatientVitals(UUID patientId) {

        if (!patientRepository.existsById(patientId)) {
            throw new RuntimeException("Patient not found: " + patientId);
        }

        return vitalRepository.findByPatient_IdOrderByRecordedAtDesc(patientId);
    }

    @Transactional(readOnly = true)
    public java.util.List<Vital> getPatientVitalsByType(
            UUID patientId,
            VitalType type) {

        if (!patientRepository.existsById(patientId)) {
            throw new RuntimeException("Patient not found: " + patientId);
        }

        return vitalRepository
                .findByPatient_IdAndTypeOrderByRecordedAtDesc(patientId, type);
    }
}
