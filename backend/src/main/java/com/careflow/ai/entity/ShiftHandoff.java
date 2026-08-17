package com.careflow.ai.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "shift_handoffs")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ShiftHandoff {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_nurse_id")
    private User fromNurse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_nurse_id")
    private User toNurse;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "pending_tasks", columnDefinition = "TEXT")
    private String pendingTasks;

    @Column(name = "observations", columnDefinition = "TEXT")
    private String observations;

    @Column(name = "completed_tasks", columnDefinition = "TEXT")
    private String completedTasks;

    @Column(name = "next_shift_instructions", columnDefinition = "TEXT")
    private String nextShiftInstructions;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    public ShiftHandoff() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public User getFromNurse() { return fromNurse; }
    public void setFromNurse(User fromNurse) { this.fromNurse = fromNurse; }

    public User getToNurse() { return toNurse; }
    public void setToNurse(User toNurse) { this.toNurse = toNurse; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPendingTasks() { return pendingTasks; }
    public void setPendingTasks(String pendingTasks) { this.pendingTasks = pendingTasks; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public String getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(String completedTasks) { this.completedTasks = completedTasks; }

    public String getNextShiftInstructions() { return nextShiftInstructions; }
    public void setNextShiftInstructions(String nextShiftInstructions) { this.nextShiftInstructions = nextShiftInstructions; }

    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }

    public LocalDate getShiftDate() { return shiftDate; }
    public void setShiftDate(LocalDate shiftDate) { this.shiftDate = shiftDate; }
}
