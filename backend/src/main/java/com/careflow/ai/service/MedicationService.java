package com.careflow.ai.service;

import com.careflow.ai.entity.EventType;
import com.careflow.ai.entity.MedStatus;
import com.careflow.ai.entity.Medication;
import com.careflow.ai.entity.Patient;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.MedicationRepository;
import com.careflow.ai.repository.PatientRepository;
import com.careflow.ai.repository.UserRepository;
import com.careflow.ai.security.PatientAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class MedicationService {

    private final MedicationRepository medicationRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PatientAuthorizationService patientAuthorizationService;
    private final TimelineService timelineService;
    private final AuditLogService auditLogService;

    public MedicationService(
            MedicationRepository medicationRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            PatientAuthorizationService patientAuthorizationService,
            TimelineService timelineService,
            AuditLogService auditLogService) {
        this.medicationRepository = medicationRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.patientAuthorizationService = patientAuthorizationService;
        this.timelineService = timelineService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Medication prescribeMedication(
            UUID patientId,
            String name,
            String dosage,
            String frequency,
            LocalDate startDate,
            LocalDate endDate) {
        return prescribeMedication(patientId, name, dosage, frequency,
                startDate, endDate, false);
    }

    private void ensurePatientAccessForMedication(UUID medicationId) {
        Medication med = medicationRepository.findById(medicationId)
                .orElseThrow(() -> new RuntimeException(
                        "Medication not found: " + medicationId));
        patientAuthorizationService.ensurePatientAccess(med.getPatient().getId());
    }

    /**
     * Phase 5B: Overloaded method with 'important' flag for remote verification.
     */
    @Transactional
    public Medication prescribeMedication(
            UUID patientId,
            String name,
            String dosage,
            String frequency,
            LocalDate startDate,
            LocalDate endDate,
            boolean important) {

        patientAuthorizationService.ensurePatientAccess(patientId);

        User doctor = resolveCurrentUser();

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found: " + patientId));

        Medication medication = new Medication();
        medication.setPatient(patient);
        medication.setPrescribedBy(doctor);
        medication.setName(name);
        medication.setDosage(dosage);
        medication.setFrequency(frequency);
        medication.setStartDate(startDate);
        medication.setEndDate(endDate);
        medication.setStatus(MedStatus.ACTIVE);
        medication.setImportant(important);

        Medication saved = medicationRepository.save(medication);

        String description = "Medication prescribed: "
                + saved.getName()
                + " "
                + saved.getDosage()
                + ", frequency: "
                + saved.getFrequency()
                + (important ? " [IMPORTANT - requires remote verification]" : "");

        timelineService.appendEvent(
                patientId,
                EventType.MEDICATION,
                description,
                doctor.getId()
        );

        auditLogService.log(
                doctor.getId(),
                "PRESCRIBE_MEDICATION",
                "Medication",
                saved.getId(),
                description
        );

        return saved;
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

    @Transactional
    public Medication discontinueMedication(UUID medicationId) {

        ensurePatientAccessForMedication(medicationId);

        User actor = resolveCurrentUser();

        Medication medication = medicationRepository.findById(medicationId)
                .orElseThrow(() -> new RuntimeException(
                        "Medication not found: " + medicationId));

        medication.setStatus(MedStatus.DISCONTINUED);

        Medication saved = medicationRepository.save(medication);

        String description = "Medication discontinued: " + saved.getName();

        timelineService.appendEvent(
                saved.getPatient().getId(),
                EventType.MEDICATION,
                description,
                actor.getId()
        );

        auditLogService.log(
                actor.getId(),
                "DISCONTINUE_MEDICATION",
                "Medication",
                saved.getId(),
                description
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Medication> getPatientMedications(UUID patientId) {

        patientAuthorizationService.ensurePatientAccess(patientId);

        if (!patientRepository.existsById(patientId)) {
            throw new RuntimeException("Patient not found: " + patientId);
        }

        return medicationRepository.findByPatient_IdOrderByStartDateDesc(patientId);
    }

    @Transactional(readOnly = true)
    public Medication getMedication(UUID medicationId) {

        ensurePatientAccessForMedication(medicationId);

        return medicationRepository.findById(medicationId)
                .orElseThrow(() -> new RuntimeException(
                        "Medication not found: " + medicationId));
    }
}