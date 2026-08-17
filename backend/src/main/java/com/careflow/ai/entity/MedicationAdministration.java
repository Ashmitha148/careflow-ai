package com.careflow.ai.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "medication_administration")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MedicationAdministration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administered_by")
    private User administeredBy;

    @Column(name = "administered_at")
    private LocalDateTime administeredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AdminStatus status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_attachment_id")
    private FileAttachment verificationVideo;

    public MedicationAdministration() {}

    public MedicationAdministration(UUID id, Medication medication,
                                    User administeredBy,
                                    LocalDateTime administeredAt,
                                    AdminStatus status,
                                    String notes,
                                    FileAttachment verificationVideo) {
        this.id = id;
        this.medication = medication;
        this.administeredBy = administeredBy;
        this.administeredAt = administeredAt;
        this.status = status;
        this.notes = notes;
        this.verificationVideo = verificationVideo;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Medication getMedication() { return medication; }
    public void setMedication(Medication medication) { this.medication = medication; }

    public User getAdministeredBy() { return administeredBy; }
    public void setAdministeredBy(User administeredBy) { this.administeredBy = administeredBy; }

    public LocalDateTime getAdministeredAt() { return administeredAt; }
    public void setAdministeredAt(LocalDateTime administeredAt) { this.administeredAt = administeredAt; }

    public AdminStatus getStatus() { return status; }
    public void setStatus(AdminStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public FileAttachment getVerificationVideo() { return verificationVideo; }
    public void setVerificationVideo(FileAttachment verificationVideo) { this.verificationVideo = verificationVideo; }

    @PrePersist
    protected void onCreate() {
        if (administeredAt == null) {
            administeredAt = LocalDateTime.now();
        }
    }
}