package com.careflow.ai.service;

import com.careflow.ai.dto.HospitalDto.CreateHospitalRequest;
import com.careflow.ai.dto.HospitalDto.HospitalResponse;
import com.careflow.ai.dto.HospitalDto.StaffMemberResponse;
import com.careflow.ai.entity.Hospital;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.HospitalRepository;
import com.careflow.ai.repository.PatientRepository;
import com.careflow.ai.repository.UserRepository;
import com.careflow.ai.security.CustomUserDetails;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Phase 8 — Hospital (tenant) administration.
 *
 * Facility onboarding and the "tenant admin" lever: moving an existing staff
 * member between hospitals. Every mutation is audit-logged; validation errors
 * surface as 400s through the global handler; authorization is enforced at the
 * controller with {@code hasRole('ADMIN')}.
 */
@Service
public class HospitalService {

    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final AuditLogService auditLogService;

    public HospitalService(HospitalRepository hospitalRepository,
                           UserRepository userRepository,
                           PatientRepository patientRepository,
                           AuditLogService auditLogService) {
        this.hospitalRepository = hospitalRepository;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<HospitalResponse> list() {
        return hospitalRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(h -> HospitalResponse.builder()
                        .id(h.getId())
                        .name(h.getName())
                        .code(h.getCode())
                        .patientCount(patientRepository.countByHospital_Id(h.getId()))
                        .staffCount(userRepository.countByHospital_Id(h.getId()))
                        .createdAt(h.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public HospitalResponse create(CreateHospitalRequest request) {
        String name = request.getName() == null ? "" : request.getName().trim();
        String code = request.getCode() == null ? "" : request.getCode().trim().toUpperCase();

        if (name.isEmpty() || code.isEmpty()) {
            throw new RuntimeException("Hospital name and code are required");
        }
        if (hospitalRepository.existsByCode(code)) {
            throw new RuntimeException("Hospital code already in use: " + code);
        }
        boolean nameTaken = hospitalRepository.findAll().stream()
                .anyMatch(h -> h.getName().equalsIgnoreCase(name));
        if (nameTaken) {
            throw new RuntimeException("Hospital name already in use: " + name);
        }

        Hospital hospital = hospitalRepository.save(new Hospital(null, name, code, null));

        User actor = resolveCurrentUser();
        auditLogService.log(actor.getId(), "CREATE_HOSPITAL", "Hospital",
                hospital.getId(), "name=" + name + ", code=" + code);

        return HospitalResponse.builder()
                .id(hospital.getId())
                .name(hospital.getName())
                .code(hospital.getCode())
                .patientCount(0)
                .staffCount(0)
                .createdAt(hospital.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<StaffMemberResponse> listStaff(UUID hospitalId) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new RuntimeException("Hospital not found: " + hospitalId));
        return userRepository.findByHospital_Id(hospital.getId()).stream()
                .map(this::toStaffResponse)
                .toList();
    }

    /**
     * Moves an existing staff member to the given hospital — the tenant-admin
     * lever. The user's data follows: from this moment the tenancy guard
     * (Phase 7) scopes them to the NEW hospital and cuts access to patients of
     * the old one.
     */
    @Transactional
    public StaffMemberResponse assignStaff(UUID hospitalId, UUID userId) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new RuntimeException("Hospital not found: " + hospitalId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        String previousHospital = user.getHospital() != null
                ? user.getHospital().getName() : "unscoped";
        user.setHospital(hospital);
        userRepository.save(user);

        User actor = resolveCurrentUser();
        auditLogService.log(actor.getId(), "ASSIGN_HOSPITAL_STAFF", "User",
                user.getId(), "user=" + user.getEmail() + ", from=" + previousHospital
                        + ", to=" + hospital.getName());

        return toStaffResponse(user);
    }

    private StaffMemberResponse toStaffResponse(User user) {
        Hospital hospital = user.getHospital();
        return StaffMemberResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .hospitalId(hospital != null ? hospital.getId() : null)
                .hospitalName(hospital != null ? hospital.getName() : null)
                .build();
    }

    private User resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails details)) {
            throw new RuntimeException("Authentication required");
        }
        return details.getUser();
    }
}
