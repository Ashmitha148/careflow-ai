package com.careflow.ai.controller;

import com.careflow.ai.dto.PatientDto;
import com.careflow.ai.entity.Patient;
import com.careflow.ai.entity.Role;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.PatientAccessRepository;
import com.careflow.ai.repository.PatientRepository;
import com.careflow.ai.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patients")
public class PatientController {

    private final PatientRepository patientRepository;
    private final PatientAccessRepository patientAccessRepository;
    private final UserRepository userRepository;

    public PatientController(
            PatientRepository patientRepository,
            PatientAccessRepository patientAccessRepository,
            UserRepository userRepository) {
        this.patientRepository = patientRepository;
        this.patientAccessRepository = patientAccessRepository;
        this.userRepository = userRepository;
    }

    /**
     * Returns patients accessible to the currently authenticated user.
     *
     * PATIENT    -> own patient record
     * CAREGIVER  -> patients linked through PatientAccess
     * READ_ONLY  -> patients linked through PatientAccess
     * NURSE      -> assigned patients
     * DOCTOR     -> assigned patients
     * ADMIN      -> all patients
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<PatientDto.PatientResponse> getMyPatients(
            Principal principal) {

        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Patient> patients = switch (user.getRole()) {

            case PATIENT, CAREGIVER, READ_ONLY -> {
                var access = patientAccessRepository.findByUser_Id(user.getId());

                yield access.stream()
                        .map(accessRecord -> accessRecord.getPatient())
                        .filter(patient -> patient != null)
                        .toList();
            }

            case NURSE ->
                    patientRepository.findByAssignedNurse_Id(user.getId());

            case DOCTOR ->
                    patientRepository.findByAssignedDoctor_Id(user.getId());

            case ADMIN ->
                    patientRepository.findAll();
        };

        return patients.stream()
                .map(this::toPatientResponse)
                .toList();
    }

    /**
     * Returns one patient if the authenticated user is authorized
     * to access that patient.
     */
    @GetMapping("/{patientId}")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public PatientDto.PatientResponse getPatient(
            @PathVariable UUID patientId,
            Principal principal) {

        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        if (!isAuthorizedForPatient(user, patient)) {
            throw new RuntimeException("Not authorized to view this patient");
        }

        return toPatientResponse(patient);
    }

    /**
     * Converts the JPA Patient entity into a DTO.
     *
     * Important:
     * We do NOT return the Patient entity directly because it contains
     * lazy-loaded Hibernate relationships. Returning the entity directly
     * can cause ByteBuddyInterceptor serialization errors.
     */
    private PatientDto.PatientResponse toPatientResponse(
            Patient patient) {

        return PatientDto.PatientResponse.builder()
                .id(patient.getId())
                .mrn(patient.getMrn())
                .name(patient.getName())
                .dob(patient.getDob())
                .gender(patient.getGender())
                .contactInfo(patient.getContactInfo())

                .assignedDoctorId(
                        patient.getAssignedDoctor() != null
                                ? patient.getAssignedDoctor().getId()
                                : null)

                .assignedDoctorName(
                        patient.getAssignedDoctor() != null
                                ? patient.getAssignedDoctor().getFullName()
                                : null)

                .assignedNurseId(
                        patient.getAssignedNurse() != null
                                ? patient.getAssignedNurse().getId()
                                : null)

                .assignedNurseName(
                        patient.getAssignedNurse() != null
                                ? patient.getAssignedNurse().getFullName()
                                : null)

                .createdAt(patient.getCreatedAt())
                .build();
    }

    /**
     * Checks whether a user is allowed to access a patient.
     */
    private boolean isAuthorizedForPatient(
            User user,
            Patient patient) {

        // Admin can access everything.
        if (user.getRole() == Role.ADMIN) {
            return true;
        }

        // Nurse can access assigned patients.
        if (user.getRole() == Role.NURSE
                && patient.getAssignedNurse() != null
                && patient.getAssignedNurse()
                        .getId()
                        .equals(user.getId())) {
            return true;
        }

        // Doctor can access assigned patients.
        if (user.getRole() == Role.DOCTOR
                && patient.getAssignedDoctor() != null
                && patient.getAssignedDoctor()
                        .getId()
                        .equals(user.getId())) {
            return true;
        }

        // Patient / Caregiver / Read-only access through PatientAccess.
        var access = patientAccessRepository
                .findByUser_Id(user.getId());

        return access.stream()
                .anyMatch(accessRecord ->
                        accessRecord.getPatient() != null
                                && accessRecord.getPatient()
                                        .getId()
                                        .equals(patient.getId()));
    }
}