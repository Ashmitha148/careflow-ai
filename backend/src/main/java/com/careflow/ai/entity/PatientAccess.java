package com.careflow.ai.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "patient_access")
public class PatientAccess {

    @EmbeddedId
    private PatientAccessId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("patientId")
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "relationship", nullable = false)
    private String relationship;

    public PatientAccess() {}

    public PatientAccess(PatientAccessId id, Patient patient, User user, String relationship) {
        this.id = id;
        this.patient = patient;
        this.user = user;
        this.relationship = relationship;
    }

    public PatientAccessId getId() { return id; }
    public void setId(PatientAccessId id) { this.id = id; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }

    public static PatientAccessBuilder builder() {
        return new PatientAccessBuilder();
    }

    public static class PatientAccessBuilder {
        private PatientAccessId id;
        private Patient patient;
        private User user;
        private String relationship;

        PatientAccessBuilder() {}

        public PatientAccessBuilder id(PatientAccessId id) { this.id = id; return this; }
        public PatientAccessBuilder patient(Patient patient) { this.patient = patient; return this; }
        public PatientAccessBuilder user(User user) { this.user = user; return this; }
        public PatientAccessBuilder relationship(String relationship) { this.relationship = relationship; return this; }

        public PatientAccess build() {
            return new PatientAccess(id, patient, user, relationship);
        }
    }
}
