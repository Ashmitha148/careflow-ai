package com.careflow.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "diagnoses")
public class Diagnosis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private User doctor;

    @Column(name = "\"condition\"", nullable = false)
    private String condition;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "diagnosed_at")
    private LocalDateTime diagnosedAt;

    public Diagnosis() {}

    public Diagnosis(UUID id, Patient patient, User doctor, String condition, String notes, LocalDateTime diagnosedAt) {
        this.id = id;
        this.patient = patient;
        this.doctor = doctor;
        this.condition = condition;
        this.notes = notes;
        this.diagnosedAt = diagnosedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public User getDoctor() { return doctor; }
    public void setDoctor(User doctor) { this.doctor = doctor; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getDiagnosedAt() { return diagnosedAt; }
    public void setDiagnosedAt(LocalDateTime diagnosedAt) { this.diagnosedAt = diagnosedAt; }

    @PrePersist
    protected void onCreate() {
        if (diagnosedAt == null) {
            diagnosedAt = LocalDateTime.now();
        }
    }

    public static DiagnosisBuilder builder() {
        return new DiagnosisBuilder();
    }

    public static class DiagnosisBuilder {
        private UUID id;
        private Patient patient;
        private User doctor;
        private String condition;
        private String notes;
        private LocalDateTime diagnosedAt;

        DiagnosisBuilder() {}

        public DiagnosisBuilder id(UUID id) { this.id = id; return this; }
        public DiagnosisBuilder patient(Patient patient) { this.patient = patient; return this; }
        public DiagnosisBuilder doctor(User doctor) { this.doctor = doctor; return this; }
        public DiagnosisBuilder condition(String condition) { this.condition = condition; return this; }
        public DiagnosisBuilder notes(String notes) { this.notes = notes; return this; }
        public DiagnosisBuilder diagnosedAt(LocalDateTime diagnosedAt) { this.diagnosedAt = diagnosedAt; return this; }

        public Diagnosis build() {
            return new Diagnosis(id, patient, doctor, condition, notes, diagnosedAt);
        }
    }
}
