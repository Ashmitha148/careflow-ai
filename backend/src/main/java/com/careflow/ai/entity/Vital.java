package com.careflow.ai.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vitals")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Vital {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "\"type\"", nullable = false)
    private VitalType type;

    @Column(name = "\"value\"", nullable = false)
    private String value;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    public Vital() {}

    public Vital(UUID id, Patient patient, User recordedBy, VitalType type,
                 String value, LocalDateTime recordedAt) {
        this.id = id;
        this.patient = patient;
        this.recordedBy = recordedBy;
        this.type = type;
        this.value = value;
        this.recordedAt = recordedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public User getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(User recordedBy) {
        this.recordedBy = recordedBy;
    }

    public VitalType getType() {
        return type;
    }

    public void setType(VitalType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
    }
}
