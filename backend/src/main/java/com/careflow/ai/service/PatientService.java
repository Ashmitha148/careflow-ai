package com.careflow.ai.service;

import com.careflow.ai.dto.PatientDto.PatientResponse;
import com.careflow.ai.dto.PatientDto.RegisterRequest;
import com.careflow.ai.dto.PatientDto.UpdateRequest;
import com.careflow.ai.entity.Patient;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.PatientRepository;
import com.careflow.ai.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class PatientService {

    private static final DateTimeFormatter MRN_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public PatientService(PatientRepository patientRepository,
                          UserRepository userRepository,
                          AuditLogService auditLogService) {
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public PatientResponse register(RegisterRequest request, UUID actorUserId) {
        Patient patient = Patient.builder()
                .mrn(generateMrn())
                .name(request.getName())
                .dob(request.getDob())
                .gender(request.getGender())
                .contactInfo(request.getContactInfo())
                .assignedDoctor(resolveUser(request.getAssignedDoctorId()))
                .assignedNurse(resolveUser(request.getAssignedNurseId()))
                .build();

        patient = patientRepository.save(patient);

        auditLogService.log(
                actorUserId,
                "PATIENT_REGISTERED",
                "Patient",
                patient.getId(),
                "mrn=" + patient.getMrn() + ", name=" + patient.getName()
        );

        return toResponse(patient);
    }

    @Transactional
    public PatientResponse update(UUID patientId, UpdateRequest request, UUID actorUserId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found: " + patientId));

        if (request.getName() != null) {
            patient.setName(request.getName());
        }
        if (request.getDob() != null) {
            patient.setDob(request.getDob());
        }
        if (request.getGender() != null) {
            patient.setGender(request.getGender());
        }
        if (request.getContactInfo() != null) {
            patient.setContactInfo(request.getContactInfo());
        }
        if (request.getAssignedDoctorId() != null) {
            patient.setAssignedDoctor(resolveUser(request.getAssignedDoctorId()));
        }
        if (request.getAssignedNurseId() != null) {
            patient.setAssignedNurse(resolveUser(request.getAssignedNurseId()));
        }

        patient = patientRepository.save(patient);

        auditLogService.log(
                actorUserId,
                "PATIENT_UPDATED",
                "Patient",
                patient.getId(),
                "mrn=" + patient.getMrn()
        );

        return toResponse(patient);
    }

    @Transactional(readOnly = true)
    public PatientResponse getById(UUID patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found: " + patientId));
        return toResponse(patient);
    }

    @Transactional(readOnly = true)
    public Page<PatientResponse> search(String query, Pageable pageable) {
        String normalized = query == null ? "" : query.trim();
        return patientRepository.searchByNameOrMrn(normalized, pageable).map(this::toResponse);
    }

    private String generateMrn() {
        String prefix = "MRN-" + LocalDate.now().format(MRN_DATE) + "-";
        int sequence = (int) patientRepository.countByMrnStartingWith(prefix) + 1;
        String mrn;
        do {
            mrn = prefix + String.format("%04d", sequence++);
        } while (patientRepository.existsByMrn(mrn));
        return mrn;
    }

    private User resolveUser(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    private PatientResponse toResponse(Patient patient) {
        User doctor = patient.getAssignedDoctor();
        User nurse = patient.getAssignedNurse();
        return PatientResponse.builder()
                .id(patient.getId())
                .mrn(patient.getMrn())
                .name(patient.getName())
                .dob(patient.getDob())
                .gender(patient.getGender())
                .contactInfo(patient.getContactInfo())
                .assignedDoctorId(doctor != null ? doctor.getId() : null)
                .assignedDoctorName(doctor != null ? doctor.getFullName() : null)
                .assignedNurseId(nurse != null ? nurse.getId() : null)
                .assignedNurseName(nurse != null ? nurse.getFullName() : null)
                .createdAt(patient.getCreatedAt())
                .build();
    }
}
