package com.careflow.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "medications")
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescribed_by")
    private User prescribedBy;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "dosage", nullable = false)
    private String dosage;

    @Column(name = "frequency", nullable = false)
    private String frequency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MedStatus status = MedStatus.ACTIVE;

    @Column(name = "important", nullable = false)
    private boolean important = false;

    public Medication() {}

    public Medication(UUID id, Patient patient, User prescribedBy, String name,
                       String dosage, String frequency, LocalDate startDate,
                       LocalDate endDate, MedStatus status, boolean important) {
        this.id = id;
        this.patient = patient;
        this.prescribedBy = prescribedBy;
        this.name = name;
        this.dosage = dosage;
        this.frequency = frequency;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.important = important;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public User getPrescribedBy() { return prescribedBy; }
    public void setPrescribedBy(User prescribedBy) { this.prescribedBy = prescribedBy; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public MedStatus getStatus() { return status; }
    public void setStatus(MedStatus status) { this.status = status; }

    public boolean isImportant() { return important; }
    public void setImportant(boolean important) { this.important = important; }
}