package com.careflow.ai.service;

import com.careflow.ai.entity.AdminStatus;
import com.careflow.ai.entity.EventType;
import com.careflow.ai.entity.Medication;
import com.careflow.ai.entity.MedicationAdministration;
import com.careflow.ai.entity.Notification;
import com.careflow.ai.entity.NotificationType;
import com.careflow.ai.entity.Patient;
import com.careflow.ai.entity.Role;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.MedicationAdministrationRepository;
import com.careflow.ai.repository.MedicationRepository;
import com.careflow.ai.repository.NotificationRepository;
import com.careflow.ai.repository.UserRepository;
import com.careflow.ai.repository.PatientAccessRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MedicationAdministrationService {

    private final MedicationAdministrationRepository administrationRepository;
    private final MedicationRepository medicationRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PatientAccessRepository patientAccessRepository;
    private final TimelineService timelineService;
    private final AuditLogService auditLogService;

    public MedicationAdministrationService(
            MedicationAdministrationRepository administrationRepository,
            MedicationRepository medicationRepository,
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            TimelineService timelineService,
            AuditLogService auditLogService) {
        this.administrationRepository = administrationRepository;
        this.medicationRepository = medicationRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.timelineService = timelineService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public MedicationAdministration recordAdministration(
            UUID medicationId,
            UUID administeredById,
            AdminStatus status,
            String notes) {

        Medication medication = medicationRepository.findById(medicationId)
                .orElseThrow(() -> new RuntimeException(
                        "Medication not found: " + medicationId));

        User administeredBy = userRepository.findById(administeredById)
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + administeredById));

        MedicationAdministration administration =
                new MedicationAdministration();

        administration.setMedication(medication);
        administration.setAdministeredBy(administeredBy);
        administration.setAdministeredAt(LocalDateTime.now());
        administration.setStatus(status);
        administration.setNotes(notes);

        MedicationAdministration saved =
                administrationRepository.save(administration);

        Patient patient = medication.getPatient();

        String description =
                "Medication administration: "
                        + medication.getName()
                        + " "
                        + medication.getDosage()
                        + " ? "
                        + status
                        + (notes != null && !notes.isBlank()
                        ? " (" + notes + ")"
                        : "");

        timelineService.appendEvent(
                patient.getId(),
                EventType.MEDICATION_ADMINISTRATION,
                description,
                administeredById
        );

        auditLogService.log(
                administeredById,
                "RECORD_MEDICATION_ADMINISTRATION",
                "MedicationAdministration",
                saved.getId(),
                description
        );

        if (status == AdminStatus.MISSED) {
            handleMissedMedication(medication, administeredById);
        }

        return saved;
    }

    private void handleMissedMedication(
            Medication medication,
            UUID administeredById) {

        List<MedicationAdministration> history =
                administrationRepository
                        .findByMedication_IdOrderByAdministeredAtDesc(
                                medication.getId());

        long missedCount = history.stream()
                .filter(a -> a.getStatus() == AdminStatus.MISSED)
                .count();

        Patient patient = medication.getPatient();

        if (missedCount == 1) {

            User nurse = patient.getAssignedNurse();

            if (nurse != null) {
                createNotification(
                        nurse,
                        "Medication reminder: "
                                + medication.getName()
                                + " for patient "
                                + patient.getName()
                                + " was missed.",
                        administeredById
                );
            }

        } else if (missedCount == 2) {

            User caregiver = userRepository
                    .findFirstByRole(Role.CAREGIVER)
                    .orElse(null);

            if (caregiver != null) {
                createNotification(
                        caregiver,
                        "Medication escalation: "
                                + medication.getName()
                                + " for patient "
                                + patient.getName()
                                + " has been missed twice.",
                        administeredById
                );
            }

        } else if (missedCount >= 3) {

            User doctor = patient.getAssignedDoctor();

            if (doctor != null) {
                createNotification(
                        doctor,
                        "URGENT medication escalation: "
                                + medication.getName()
                                + " for patient "
                                + patient.getName()
                                + " has been missed "
                                + missedCount
                                + " times.",
                        administeredById
                );
            }
        }
    }

    private void createNotification(
            User user,
            String message,
            UUID triggeredByUserId) {

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(NotificationType.MISSED_MEDICATION);
        notification.setMessage(message);
        notification.setRead(false);

        notificationRepository.save(notification);

        auditLogService.log(
                triggeredByUserId,
                "MEDICATION_ESCALATION",
                "Notification",
                notification.getId(),
                message
        );
    }

    @Transactional(readOnly = true)
    public List<MedicationAdministration> getMedicationHistory(
            UUID medicationId) {

        return administrationRepository
                .findByMedication_IdOrderByAdministeredAtDesc(
                        medicationId);
    }

    @Transactional(readOnly = true)
    public List<MedicationAdministration> getAdministrationsByUser(
            UUID userId) {

        return administrationRepository
                .findByAdministeredBy_IdOrderByAdministeredAtDesc(
                        userId);
    }
}
