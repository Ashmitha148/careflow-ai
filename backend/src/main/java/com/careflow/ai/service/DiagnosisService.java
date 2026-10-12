package com.careflow.ai.service;

import com.careflow.ai.dto.DiagnosisDto.CreateRequest;
import com.careflow.ai.dto.DiagnosisDto.DiagnosisResponse;
import com.careflow.ai.dto.TimelineDto.TimelineEventResponse;
import com.careflow.ai.entity.Diagnosis;
import com.careflow.ai.entity.EventType;
import com.careflow.ai.entity.Patient;
import com.careflow.ai.entity.Role;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.DiagnosisRepository;
import com.careflow.ai.repository.PatientRepository;
import com.careflow.ai.repository.UserRepository;
import com.careflow.ai.security.CustomUserDetails;
import com.careflow.ai.security.PatientAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DiagnosisService {

    private final DiagnosisRepository diagnosisRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final TimelineService timelineService;
    private final AuditLogService auditLogService;
    private final PatientAuthorizationService patientAuthorizationService;

    public DiagnosisService(DiagnosisRepository diagnosisRepository,
                            PatientRepository patientRepository,
                            UserRepository userRepository,
                            TimelineService timelineService,
                            AuditLogService auditLogService,
                            PatientAuthorizationService patientAuthorizationService) {
        this.diagnosisRepository = diagnosisRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.timelineService = timelineService;
        this.auditLogService = auditLogService;
        this.patientAuthorizationService = patientAuthorizationService;
    }

    @Transactional
    public DiagnosisResponse create(CreateRequest request) {
        patientAuthorizationService.ensurePatientAccess(request.getPatientId());

        // The diagnosing doctor is always the authenticated caller — never a
        // client-supplied doctorUserId — so forged audit attribution is
        // impossible.
        User doctor = resolveCurrentUser();

        if (doctor.getRole() != Role.DOCTOR && doctor.getRole() != Role.ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only doctors (or admins) may create diagnoses");
        }

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException(
                        "Patient not found: " + request.getPatientId()));

        Diagnosis diagnosis = Diagnosis.builder()
                .patient(patient)
                .doctor(doctor)
                .condition(request.getCondition())
                .notes(request.getNotes())
                .build();

        diagnosis = diagnosisRepository.save(diagnosis);

        String description = "Diagnosed with " + request.getCondition();
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            description = description + " — " + request.getNotes();
        }

        TimelineEventResponse timelineEvent = timelineService.appendEvent(
                patient.getId(),
                EventType.DIAGNOSIS,
                description,
                doctor.getId()
        );

        auditLogService.log(
                doctor.getId(),
                "DIAGNOSIS_CREATED",
                "Diagnosis",
                diagnosis.getId(),
                "condition=" + diagnosis.getCondition() + ", timelineEventId=" + timelineEvent.getId()
        );

        return toResponse(diagnosis, timelineEvent.getId());
    }

    @Transactional(readOnly = true)
    public DiagnosisResponse getById(UUID diagnosisId) {
        Diagnosis diagnosis = diagnosisRepository.findById(diagnosisId)
                .orElseThrow(() -> new RuntimeException("Diagnosis not found: " + diagnosisId));
        patientAuthorizationService.ensurePatientAccess(diagnosis.getPatient().getId());
        return toResponse(diagnosis, null);
    }

    @Transactional(readOnly = true)
    public List<DiagnosisResponse> getByPatient(UUID patientId) {
        patientAuthorizationService.ensurePatientAccess(patientId);
        if (!patientRepository.existsById(patientId)) {
            throw new RuntimeException("Patient not found: " + patientId);
        }
        return diagnosisRepository.findByPatient_IdOrderByDiagnosedAtDesc(patientId).stream()
                .map(d -> toResponse(d, null))
                .toList();
    }

    private DiagnosisResponse toResponse(Diagnosis diagnosis, UUID timelineEventId) {
        User doctor = diagnosis.getDoctor();
        return DiagnosisResponse.builder()
                .id(diagnosis.getId())
                .patientId(diagnosis.getPatient().getId())
                .doctorId(doctor != null ? doctor.getId() : null)
                .doctorName(doctor != null ? doctor.getFullName() : null)
                .condition(diagnosis.getCondition())
                .notes(diagnosis.getNotes())
                .diagnosedAt(diagnosis.getDiagnosedAt())
                .timelineEventId(timelineEventId)
                .build();
    }

    private User resolveCurrentUser() {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof CustomUserDetails details)) {
            throw new RuntimeException("Authentication required");
        }
        return details.getUser();
    }
}
