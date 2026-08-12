package com.careflow.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mrn", nullable = false, unique = true)
    private String mrn;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "dob", nullable = false)
    private LocalDate dob;

    @Column(name = "gender", nullable = false)
    private String gender;

    @Column(name = "contact_info", columnDefinition = "TEXT")
    private String contactInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_doctor_id")
    private User assignedDoctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_nurse_id")
    private User assignedNurse;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "remote_supervision_enabled", nullable = false)
    private boolean remoteSupervisionEnabled = false;

    @Column(name = "caregiver_physically_present")
    private boolean caregiverPhysicallyPresent = false;

    public Patient() {}

    public Patient(UUID id, String mrn, String name, LocalDate dob, String gender, 
                   String contactInfo, User assignedDoctor, User assignedNurse, 
                   LocalDateTime createdAt, boolean remoteSupervisionEnabled,
                   boolean caregiverPhysicallyPresent) {
        this.id = id;
        this.mrn = mrn;
        this.name = name;
        this.dob = dob;
        this.gender = gender;
        this.contactInfo = contactInfo;
        this.assignedDoctor = assignedDoctor;
        this.assignedNurse = assignedNurse;
        this.createdAt = createdAt;
        this.remoteSupervisionEnabled = remoteSupervisionEnabled;
        this.caregiverPhysicallyPresent = caregiverPhysicallyPresent;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getMrn() { return mrn; }
    public void setMrn(String mrn) { this.mrn = mrn; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }

    public User getAssignedDoctor() { return assignedDoctor; }
    public void setAssignedDoctor(User assignedDoctor) { this.assignedDoctor = assignedDoctor; }

    public User getAssignedNurse() { return assignedNurse; }
    public void setAssignedNurse(User assignedNurse) { this.assignedNurse = assignedNurse; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isRemoteSupervisionEnabled() { return remoteSupervisionEnabled; }
    public void setRemoteSupervisionEnabled(boolean remoteSupervisionEnabled) {
        this.remoteSupervisionEnabled = remoteSupervisionEnabled;
    }

    public boolean hasCaregiverPhysicallyPresent() { return caregiverPhysicallyPresent; }
    public void setCaregiverPhysicallyPresent(boolean caregiverPhysicallyPresent) {
        this.caregiverPhysicallyPresent = caregiverPhysicallyPresent;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public static PatientBuilder builder() {
        return new PatientBuilder();
    }

    public static class PatientBuilder {
        private UUID id;
        private String mrn;
        private String name;
        private LocalDate dob;
        private String gender;
        private String contactInfo;
        private User assignedDoctor;
        private User assignedNurse;
        private LocalDateTime createdAt;
        private boolean remoteSupervisionEnabled;
        private boolean caregiverPhysicallyPresent;

        PatientBuilder() {}

        public PatientBuilder id(UUID id) { this.id = id; return this; }
        public PatientBuilder mrn(String mrn) { this.mrn = mrn; return this; }
        public PatientBuilder name(String name) { this.name = name; return this; }
        public PatientBuilder dob(LocalDate dob) { this.dob = dob; return this; }
        public PatientBuilder gender(String gender) { this.gender = gender; return this; }
        public PatientBuilder contactInfo(String contactInfo) { this.contactInfo = contactInfo; return this; }
        public PatientBuilder assignedDoctor(User assignedDoctor) { this.assignedDoctor = assignedDoctor; return this; }
        public PatientBuilder assignedNurse(User assignedNurse) { this.assignedNurse = assignedNurse; return this; }
        public PatientBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public PatientBuilder remoteSupervisionEnabled(boolean remoteSupervisionEnabled) { 
            this.remoteSupervisionEnabled = remoteSupervisionEnabled; return this; 
        }
        public PatientBuilder caregiverPhysicallyPresent(boolean caregiverPhysicallyPresent) { 
            this.caregiverPhysicallyPresent = caregiverPhysicallyPresent; return this; 
        }

        public Patient build() {
            return new Patient(id, mrn, name, dob, gender, contactInfo, 
                assignedDoctor, assignedNurse, createdAt, 
                remoteSupervisionEnabled, caregiverPhysicallyPresent);
        }
    }
}