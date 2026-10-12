package com.careflow.ai.websocket;

import com.careflow.ai.entity.User;
import com.careflow.ai.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Shared patient-level access rule for WebSocket topics and presence.
 *
 * <p>Delegates to {@link com.careflow.ai.security.PatientAuthorizationService}
 * so WebSocket authorization cannot drift from the REST authorization — both
 * enforce the identical rule: same-hospital tenant, then ADMIN, assigned
 * doctor/nurse, or an explicit {@code patient_access} grant.
 */
@Component
public class PatientAccessChecker {

    private final com.careflow.ai.security.PatientAuthorizationService patientAuthorizationService;
    private final UserRepository userRepository;

    public PatientAccessChecker(com.careflow.ai.security.PatientAuthorizationService patientAuthorizationService,
                                UserRepository userRepository) {
        this.patientAuthorizationService = patientAuthorizationService;
        this.userRepository = userRepository;
    }

    public boolean canAccess(WebSocketPrincipal principal, UUID patientId) {
        if (principal == null || principal.userId() == null || patientId == null) {
            return false;
        }

        User actor = userRepository.findById(principal.userId()).orElse(null);
        if (actor == null) {
            return false;
        }

        try {
            patientAuthorizationService.ensurePatientAccess(actor, patientId);
            return true;
        } catch (com.careflow.ai.security.PatientAccessDeniedException ex) {
            return false;
        }
    }
}