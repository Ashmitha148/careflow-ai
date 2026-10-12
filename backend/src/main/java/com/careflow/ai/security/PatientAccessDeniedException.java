package com.careflow.ai.security;

import java.util.UUID;

public class PatientAccessDeniedException extends RuntimeException {

    public PatientAccessDeniedException(UUID patientId) {
        super("Access denied for patient: " + patientId);
    }
}
