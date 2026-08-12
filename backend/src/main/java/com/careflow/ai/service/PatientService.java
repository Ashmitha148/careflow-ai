package com.careflow.ai.service;

import com.careflow.ai.dto.PatientDto.PatientResponse;
import com.careflow.ai.dto.PatientDto.RegisterRequest;
import com.careflow.ai.dto.PatientDto.UpdateRequest;
import com.careflow.ai.entity.Patient;
import com.careflow.ai.entity.PatientAccess;
import com.careflow.ai.entity.Role;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.PatientAccessRepository;
import com.careflow.ai.repository.PatientRepository;
import com.careflow.ai.repository.UserRepository;
import com.careflow.ai.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PatientService {

    private static final DateTimeFormatter MRN_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PatientAccessRepository patientAccessRepository;
    private final AuditLogService auditLogService;

    public PatientService(PatientRepository patientRepository,
                          UserRepository userRepository,
                          PatientAccessRepository patientAccessRepository,
                          AuditLogService auditLogService) {
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.patientAccessRepository = patientAccessRepository;
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
        User actor = resolveCurrentUser();
        ensureAccess(actor, patient);
        return toResponse(patient);
    }

    @Transactional(readOnly = true)
    public Page<PatientResponse> search(String query, Pageable pageable) {
        String normalized = query == null ? "" : query.trim();
        User actor = resolveCurrentUser();

        // Admins may search all patients.
        if (actor.getRole() == Role.ADMIN) {
            return patientRepository.searchByNameOrMrn(normalized, pageable).map(this::toResponse);
        }

        Set<UUID> accessibleIds = resolveAccessiblePatientIds(actor);
        if (accessibleIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return patientRepository
                .searchAccessibleByNameOrMrn(accessibleIds, normalized, pageable)
                .map(this::toResponse);
    }

    /**
     * Returns the set of patient ids the given (non-admin) user may access: those
     * they are assigned to as doctor/nurse, plus any explicit patient_access rows.
     */
    private Set<UUID> resolveAccessiblePatientIds(User actor) {
        Set<UUID> ids = new HashSet<>(patientRepository.findAccessibleIdsByAssignment(actor.getId()));
        List<PatientAccess> accesses = patientAccessRepository.findByUser_Id(actor.getId());
        for (PatientAccess access : accesses) {
            if (access != null && access.getId() != null) {
                ids.add(access.getId().getPatientId());
            }
        }
        return ids;
    }

    /**
     * Throws if the given actor cannot read this patient. Access is granted for
     * ADMIN, the assigned doctor/nurse, or an explicit patient_access row.
     */
    private void ensureAccess(User actor, Patient patient) {
        if (actor.getRole() == Role.ADMIN) {
            return;
        }
        UUID actorId = actor.getId();
        User doctor = patient.getAssignedDoctor();
        if (doctor != null && actorId.equals(doctor.getId())) {
            return;
        }
        User nurse = patient.getAssignedNurse();
        if (nurse != null && actorId.equals(nurse.getId())) {
            return;
        }
        if (patientAccessRepository.existsByPatient_IdAndUser_Id(patient.getId(), actorId)) {
            return;
        }
        throw new RuntimeException("Access denied to patient: " + patient.getId());
    }

    private User resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails details)) {
            throw new RuntimeException("Authentication required");
        }
        return details.getUser();
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
