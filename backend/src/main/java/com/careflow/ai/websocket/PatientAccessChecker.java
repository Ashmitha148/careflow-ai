package com.careflow.ai.websocket;

import com.careflow.ai.entity.Patient;
import com.careflow.ai.entity.Role;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.PatientAccessRepository;
import com.careflow.ai.repository.PatientRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Shared patient-level access rule for WebSocket topics and presence.
 *
 * Mirrors the REST-layer rule enforced in TimelineService.ensurePatientAccess:
 * ADMIN, the assigned doctor/nurse, or an explicit patient_access row may read
 * a patient's realtime data. Keeping one component here guarantees WebSocket
 * authorization cannot drift from the REST authorization.
 */
@Component
public class PatientAccessChecker {

    private final PatientRepository patientRepository;
    private final PatientAccessRepository patientAccessRepository;

    public PatientAccessChecker(PatientRepository patientRepository,
                                PatientAccessRepository patientAccessRepository) {
        this.patientRepository = patientRepository;
        this.patientAccessRepository = patientAccessRepository;
    }

    public boolean canAccess(WebSocketPrincipal principal, UUID patientId) {
        if (principal == null || principal.userId() == null || patientId == null) {
            return false;
        }
        if (Role.ADMIN.name().equals(principal.role())) {
            return true;
        }

        Patient patient = patientRepository.findById(patientId).orElse(null);
        if (patient == null) {
            return false;
        }

        UUID actorId = principal.userId();
        User doctor = patient.getAssignedDoctor();
        if (doctor != null && actorId.equals(doctor.getId())) {
            return true;
        }
        User nurse = patient.getAssignedNurse();
        if (nurse != null && actorId.equals(nurse.getId())) {
            return true;
        }
        return patientAccessRepository.existsByPatient_IdAndUser_Id(patientId, actorId);
    }
}