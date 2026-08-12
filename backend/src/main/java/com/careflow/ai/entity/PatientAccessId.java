package com.careflow.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class PatientAccessId implements Serializable {

    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "user_id")
    private UUID userId;

    public PatientAccessId() {}

    public PatientAccessId(UUID patientId, UUID userId) {
        this.patientId = patientId;
        this.userId = userId;
    }

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PatientAccessId that = (PatientAccessId) o;
        return Objects.equals(patientId, that.patientId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(patientId, userId);
    }

    public static PatientAccessIdBuilder builder() {
        return new PatientAccessIdBuilder();
    }

    public static class PatientAccessIdBuilder {
        private UUID patientId;
        private UUID userId;

        PatientAccessIdBuilder() {}

        public PatientAccessIdBuilder patientId(UUID patientId) { this.patientId = patientId; return this; }
        public PatientAccessIdBuilder userId(UUID userId) { this.userId = userId; return this; }

        public PatientAccessId build() {
            return new PatientAccessId(patientId, userId);
        }
    }
}
