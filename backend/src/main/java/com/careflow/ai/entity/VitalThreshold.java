package com.careflow.ai.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A per-patient override of the global critical-vital thresholds
 * (Phase 5 — configurable alert thresholds).
 *
 * <p>One row per (patient, vital type). Null bounds mean "no alert on that
 * side"; a patient with NO row for a vital type falls back to the global
 * defaults in {@link VitalType} / {@code VitalService}.</p>
 *
 * <p>Bound semantics per type:</p>
 * <ul>
 *   <li>{@code OXYGEN} — {@code minValue}: alert when SpO2 &lt; minValue (%)</li>
 *   <li>{@code TEMPERATURE} — {@code maxValue}: alert when temp &gt; maxValue (°F)</li>
 *   <li>{@code SUGAR} — {@code minValue}/{@code maxValue}: alert outside range (mg/dL)</li>
 *   <li>{@code BLOOD_PRESSURE} — {@code systolicMax}/{@code diastolicMax}:
 *       alert when either exceeds its bound (mmHg)</li>
 * </ul>
 */
@Entity
@Table(name = "vital_thresholds",
       uniqueConstraints = @UniqueConstraint(columnNames = {"patient_id", "vital_type"}))
public class VitalThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(name = "vital_type", nullable = false, length = 32)
    private VitalType vitalType;

    /** Alert when the parsed numeric value is BELOW this (OXYGEN, SUGAR). */
    @Column(name = "min_value")
    private Double minValue;

    /** Alert when the parsed numeric value is ABOVE this (TEMPERATURE °F, SUGAR). */
    @Column(name = "max_value")
    private Double maxValue;

    /** BLOOD_PRESSURE only: alert when systolic > this (mmHg). */
    @Column(name = "systolic_max")
    private Double systolicMax;

    /** BLOOD_PRESSURE only: alert when diastolic > this (mmHg). */
    @Column(name = "diastolic_max")
    private Double diastolicMax;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public VitalType getVitalType() { return vitalType; }
    public void setVitalType(VitalType vitalType) { this.vitalType = vitalType; }
    public Double getMinValue() { return minValue; }
    public void setMinValue(Double minValue) { this.minValue = minValue; }
    public Double getMaxValue() { return maxValue; }
    public void setMaxValue(Double maxValue) { this.maxValue = maxValue; }
    public Double getSystolicMax() { return systolicMax; }
    public void setSystolicMax(Double systolicMax) { this.systolicMax = systolicMax; }
    public Double getDiastolicMax() { return diastolicMax; }
    public void setDiastolicMax(Double diastolicMax) { this.diastolicMax = diastolicMax; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}