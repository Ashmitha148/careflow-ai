package com.careflow.ai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "shift_handoffs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}
