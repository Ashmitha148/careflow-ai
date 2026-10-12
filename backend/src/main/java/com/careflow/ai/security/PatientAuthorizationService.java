package com.careflow.ai.security;

import com.careflow.ai.entity.Patient;
import com.careflow.ai.entity.Role;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.PatientAccessRepository;
import com.careflow.ai.repository.PatientRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Single source of truth for the patient-level access rule.
 *
 * <p>The rule: the authenticated actor must belong to the same hospital
 * tenant as the target patient, AND be either an ADMIN, the patient's
 * assigned doctor, the patient's assigned nurse, or hold an explicit
 * {@code patient_access} grant row.
 *
 * <p>This component resolves the actor from {@code SecurityContextHolder}
 * — callers must never pass in a user ID from a request parameter. Every
 * patient-facing service delegates here so the authorization logic cannot
 * drift between the REST layer, the WebSocket layer
 * ({@code PatientAccessChecker}), and the timeline service.
 */
@Component
public class PatientAuthorizationService {

    private final PatientRepository patientRepository;
    private final PatientAccessRepository patientAccessRepository;

    public PatientAuthorizationService(PatientRepository patientRepository,
                                       PatientAccessRepository patientAccessRepository) {
        this.patientRepository = patientRepository;
        this.patientAccessRepository = patientAccessRepository;
    }

    /**
     * Returns silently if the currently authenticated actor may access
     * {@code patientId}, otherwise throws {@link PatientAccessDeniedException}.
     *
     * <p>This is the primary entry point for the REST layer, where the actor
     * is always the SecurityContextHolder principal.
     *
     * @param patientId the patient being accessed
     */
    public void ensurePatientAccess(UUID patientId) {
        ensurePatientAccess(resolveCurrentUser(), patientId);
    }

    /**
     * Returns silently if {@code actor} may access {@code patientId},
     * otherwise throws {@link PatientAccessDeniedException}.
     *
     * <p>This overload lets the WebSocket layer (which carries a
     * {@code WebSocketPrincipal}, not a SecurityContext) enforce the exact
     * same rule without re-implementing it.
     *
     * @param actor     the authenticated user
     * @param patientId the patient being accessed
     */
    public void ensurePatientAccess(User actor, UUID patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientAccessDeniedException(patientId));

        // Tenant boundary first: a user from a different hospital may never
        // reach this patient, regardless of role or assignment.
        if (actor.getHospital() != null && patient.getHospital() != null
                && !actor.getHospital().getId().equals(patient.getHospital().getId())) {
            throw new PatientAccessDeniedException(patientId);
        }

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
        if (patientAccessRepository.existsByPatient_IdAndUser_Id(patientId, actorId)) {
            return;
        }
        throw new PatientAccessDeniedException(patientId);
    }

    private User resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails details)) {
            throw new RuntimeException("Authentication required");
        }
        return details.getUser();
    }
}
