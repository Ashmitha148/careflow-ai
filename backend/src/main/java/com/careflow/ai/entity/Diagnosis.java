package com.careflow.ai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "diagnoses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @PrePersist
    protected void onCreate() {
        if (diagnosedAt == null) {
            diagnosedAt = LocalDateTime.now();
        }
    }
}
