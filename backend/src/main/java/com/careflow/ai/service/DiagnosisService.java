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

    public DiagnosisService(DiagnosisRepository diagnosisRepository,
                            PatientRepository patientRepository,
                            UserRepository userRepository,
                            TimelineService timelineService,
                            AuditLogService auditLogService) {
        this.diagnosisRepository = diagnosisRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.timelineService = timelineService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public DiagnosisResponse create(CreateRequest request, UUID doctorUserId) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found: " + request.getPatientId()));

        User doctor = userRepository.findById(doctorUserId)
                .orElseThrow(() -> new RuntimeException("User not found: " + doctorUserId));

        if (doctor.getRole() != Role.DOCTOR && doctor.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only doctors (or admins) may create diagnoses");
        }

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
                doctorUserId
        );

        auditLogService.log(
                doctorUserId,
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
        return toResponse(diagnosis, null);
    }

    @Transactional(readOnly = true)
    public List<DiagnosisResponse> getByPatient(UUID patientId) {
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
}
