package com.careflow.ai.service;

import com.careflow.ai.entity.AdminStatus;
import com.careflow.ai.entity.EventType;
import com.careflow.ai.entity.FileAttachment;
import com.careflow.ai.entity.Medication;
import com.careflow.ai.entity.MedicationAdministration;
import com.careflow.ai.entity.Notification;
import com.careflow.ai.entity.NotificationType;
import com.careflow.ai.entity.Patient;
import com.careflow.ai.entity.PatientAccess;
import com.careflow.ai.entity.Role;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.MedicationAdministrationRepository;
import com.careflow.ai.repository.MedicationRepository;
import com.careflow.ai.repository.NotificationRepository;
import com.careflow.ai.repository.PatientAccessRepository;
import com.careflow.ai.repository.UserRepository;
import com.careflow.ai.security.PatientAuthorizationService;
import com.careflow.ai.websocket.RealtimeEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MedicationAdministrationService {

    private final MedicationAdministrationRepository administrationRepository;
    private final MedicationRepository medicationRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PatientAccessRepository patientAccessRepository;
    private final PatientAuthorizationService patientAuthorizationService;
    private final FileAttachmentService fileAttachmentService;
    private final TimelineService timelineService;
    private final AuditLogService auditLogService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public MedicationAdministrationService(
            MedicationAdministrationRepository administrationRepository,
            MedicationRepository medicationRepository,
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            PatientAccessRepository patientAccessRepository,
            PatientAuthorizationService patientAuthorizationService,
            FileAttachmentService fileAttachmentService,
            TimelineService timelineService,
            AuditLogService auditLogService,
            RealtimeEventPublisher realtimeEventPublisher) {
        this.administrationRepository = administrationRepository;
        this.medicationRepository = medicationRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.patientAccessRepository = patientAccessRepository;
        this.patientAuthorizationService = patientAuthorizationService;
        this.fileAttachmentService = fileAttachmentService;
        this.timelineService = timelineService;
        this.auditLogService = auditLogService;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Transactional
    public MedicationAdministration recordAdministration(
            UUID medicationId,
            AdminStatus status,
            String notes) {

        ensurePatientAccessForMedication(medicationId);

        User administeredBy = resolveCurrentUser();

        Medication medication = medicationRepository.findById(medicationId)
                .orElseThrow(() -> new RuntimeException(
                        "Medication not found: " + medicationId));

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
                        + " - "
                        + status
                        + (notes != null && !notes.isBlank()
                        ? " (" + notes + ")"
                        : "");

        timelineService.appendEvent(
                patient.getId(),
                EventType.MEDICATION_ADMINISTRATION,
                description,
                administeredBy.getId()
        );

        auditLogService.log(
                administeredBy.getId(),
                "RECORD_MEDICATION_ADMINISTRATION",
                "MedicationAdministration",
                saved.getId(),
                description
        );

        if (status == AdminStatus.MISSED) {
            handleMissedMedication(medication, administeredBy.getId());
        }

        return saved;
    }

    @Transactional
    public MedicationAdministration recordAdministrationWithVideoVerification(
            UUID medicationId,
            AdminStatus status,
            String notes,
            MultipartFile videoFile) {

        ensurePatientAccessForMedication(medicationId);

        Medication medication = medicationRepository.findById(medicationId)
                .orElseThrow(() -> new RuntimeException(
                        "Medication not found: " + medicationId));

        Patient patient = medication.getPatient();
        boolean requiresVerification = isRemoteVerificationRequired(patient, medication);

        User administeredBy = resolveCurrentUser();

        if (requiresVerification) {
            if (videoFile == null || videoFile.isEmpty()) {
                throw new IllegalArgumentException(
                        "Video verification is required for this medication. Please record a verification video.");
            }
        }

        MedicationAdministration administration = new MedicationAdministration();
        administration.setMedication(medication);
        administration.setAdministeredBy(administeredBy);
        administration.setAdministeredAt(LocalDateTime.now());
        administration.setStatus(status);
        administration.setNotes(notes);

        FileAttachment verificationVideo = null;
        if (videoFile != null && !videoFile.isEmpty()) {
            verificationVideo = fileAttachmentService.uploadFile(
                    patient.getId(),
                    videoFile
            );
            administration.setVerificationVideo(verificationVideo);
        }

        MedicationAdministration saved =
                administrationRepository.save(administration);

        String description =
                "Medication administration"
                        + (requiresVerification ? " with video verification" : "")
                        + ": " + medication.getName()
                        + " " + medication.getDosage()
                        + " - " + status
                        + (notes != null && !notes.isBlank()
                        ? " (" + notes + ")"
                        : "");

        timelineService.appendEvent(
                patient.getId(),
                EventType.MEDICATION_ADMINISTRATION,
                description,
                administeredBy.getId()
        );

        auditLogService.log(
                administeredBy.getId(),
                requiresVerification
                        ? "RECORD_MEDICATION_ADMINISTRATION_WITH_VIDEO"
                        : "RECORD_MEDICATION_ADMINISTRATION",
                "MedicationAdministration",
                saved.getId(),
                description
        );

        if (requiresVerification && status != AdminStatus.MISSED) {
            notifyAuthorizedUsersOfVerification(
                    patient,
                    medication,
                    administeredBy,
                    saved.getId()
            );
        }

        if (status == AdminStatus.MISSED) {
            handleMissedMedication(medication, administeredBy.getId());
        }

        return saved;
    }

    public boolean isRemoteVerificationRequired(Patient patient, Medication medication) {
        return patient.isRemoteSupervisionEnabled()
                && !patient.hasCaregiverPhysicallyPresent()
                && medication.isImportant();
    }

    private void ensurePatientAccessForMedication(UUID medicationId) {
        Medication med = medicationRepository.findById(medicationId)
                .orElseThrow(() -> new RuntimeException(
                        "Medication not found: " + medicationId));
        patientAuthorizationService.ensurePatientAccess(med.getPatient().getId());
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

    private void notifyAuthorizedUsersOfVerification(
            Patient patient,
            Medication medication,
            User verifiedBy,
            UUID administrationId) {

        List<PatientAccess> accessList = patientAccessRepository
                .findByPatient_Id(patient.getId());

        List<User> authorizedUsers = accessList.stream()
                .map(PatientAccess::getUser)
                .distinct()
                .collect(Collectors.toList());

        if (patient.getAssignedDoctor() != null) {
            authorizedUsers.add(patient.getAssignedDoctor());
        }
        if (patient.getAssignedNurse() != null) {
            authorizedUsers.add(patient.getAssignedNurse());
        }

        authorizedUsers = authorizedUsers.stream()
                .distinct()
                .filter(u -> !u.getId().equals(verifiedBy.getId()))
                .collect(Collectors.toList());

        String message = "Medication verification confirmed for "
                + patient.getName()
                + ": " + medication.getName()
                + " " + medication.getDosage()
                + " (verified by " + verifiedBy.getFullName() + ")";

        for (User user : authorizedUsers) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType(NotificationType.MISSED_MEDICATION);
            notification.setMessage(message);
            notification.setRead(false);
            notificationRepository.save(notification);

            auditLogService.log(
                    verifiedBy.getId(),
                    "NOTIFY_VERIFICATION",
                    "Notification",
                    notification.getId(),
                    "Notified " + user.getFullName() + " about medication verification"
            );
        }
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
            List<PatientAccess> accesses = patientAccessRepository.findByPatient_Id(patient.getId());
            boolean notified = false;
            for (PatientAccess access : accesses) {
                if (access.getUser() != null && access.getUser().getRole() == Role.CAREGIVER) {
                    createNotification(
                            access.getUser(),
                            "Medication escalation: "
                                    + medication.getName()
                                    + " for patient "
                                    + patient.getName()
                                    + " has been missed twice.",
                            administeredById
                    );
                    notified = true;
                }
            }
            if (!notified && patient.getAssignedNurse() != null) {
                createNotification(
                        patient.getAssignedNurse(),
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

        realtimeEventPublisher.publishNotification(user.getId(), notification);

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

        ensurePatientAccessForMedication(medicationId);

        return administrationRepository
                .findByMedication_IdOrderByAdministeredAtDesc(
                        medicationId);
    }

    @Transactional(readOnly = true)
    public List<MedicationAdministration> getAdministrationsByUser() {
        User actor = resolveCurrentUser();
        return administrationRepository
                .findByAdministeredBy_IdOrderByAdministeredAtDesc(
                        actor.getId());
    }

    @Transactional(readOnly = true)
    public MedicationAdministration getAdministrationWithVideoCheck(
            UUID administrationId) {

        MedicationAdministration administration = administrationRepository
                .findById(administrationId)
                .orElseThrow(() -> new RuntimeException(
                        "Administration not found: " + administrationId));

        Patient patient = administration.getMedication().getPatient();

        patientAuthorizationService.ensurePatientAccess(patient.getId());

        return administration;
    }
}