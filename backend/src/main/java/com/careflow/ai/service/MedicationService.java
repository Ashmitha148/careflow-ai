package com.careflow.ai.service;

import com.careflow.ai.entity.EventType;
import com.careflow.ai.entity.MedStatus;
import com.careflow.ai.entity.Medication;
import com.careflow.ai.entity.Patient;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.MedicationRepository;
import com.careflow.ai.repository.PatientRepository;
import com.careflow.ai.repository.UserRepository;
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
    private final TimelineService timelineService;
    private final AuditLogService auditLogService;

    public MedicationService(
            MedicationRepository medicationRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            TimelineService timelineService,
            AuditLogService auditLogService) {
        this.medicationRepository = medicationRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.timelineService = timelineService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Medication prescribeMedication(
            UUID patientId,
            UUID doctorId,
            String name,
            String dosage,
            String frequency,
            LocalDate startDate,
            LocalDate endDate) {

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found: " + patientId));

        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found: " + doctorId));

        Medication medication = new Medication();
        medication.setPatient(patient);
        medication.setPrescribedBy(doctor);
        medication.setName(name);
        medication.setDosage(dosage);
        medication.setFrequency(frequency);
        medication.setStartDate(startDate);
        medication.setEndDate(endDate);
        medication.setStatus(MedStatus.ACTIVE);

        Medication saved = medicationRepository.save(medication);

        String description = "Medication prescribed: "
                + saved.getName()
                + " "
                + saved.getDosage()
                + ", frequency: "
                + saved.getFrequency();

        timelineService.appendEvent(
                patientId,
                EventType.MEDICATION,
                description,
                doctorId
        );

        auditLogService.log(
                doctorId,
                "PRESCRIBE_MEDICATION",
                "Medication",
                saved.getId(),
                description
        );

        return saved;
    }

    @Transactional
    public Medication discontinueMedication(UUID medicationId, UUID userId) {

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
                userId
        );

        auditLogService.log(
                userId,
                "DISCONTINUE_MEDICATION",
                "Medication",
                saved.getId(),
                description
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Medication> getPatientMedications(UUID patientId) {

        if (!patientRepository.existsById(patientId)) {
            throw new RuntimeException("Patient not found: " + patientId);
        }

        return medicationRepository.findByPatient_IdOrderByStartDateDesc(patientId);
    }

    @Transactional(readOnly = true)
    public Medication getMedication(UUID medicationId) {

        return medicationRepository.findById(medicationId)
                .orElseThrow(() -> new RuntimeException(
                        "Medication not found: " + medicationId));
    }
}
