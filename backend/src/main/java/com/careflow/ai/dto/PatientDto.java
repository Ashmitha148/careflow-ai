package com.careflow.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class PatientDto {

    public static class RegisterRequest {
        @NotBlank(message = "Name is required")
        private String name;

        @NotNull(message = "Date of birth is required")
        private LocalDate dob;

        @NotBlank(message = "Gender is required")
        private String gender;

        private String contactInfo;
        private UUID assignedDoctorId;
        private UUID assignedNurseId;

        public RegisterRequest() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public LocalDate getDob() { return dob; }
        public void setDob(LocalDate dob) { this.dob = dob; }

        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }

        public String getContactInfo() { return contactInfo; }
        public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }

        public UUID getAssignedDoctorId() { return assignedDoctorId; }
        public void setAssignedDoctorId(UUID assignedDoctorId) { this.assignedDoctorId = assignedDoctorId; }

        public UUID getAssignedNurseId() { return assignedNurseId; }
        public void setAssignedNurseId(UUID assignedNurseId) { this.assignedNurseId = assignedNurseId; }
    }

    public static class UpdateRequest {
        private String name;
        private LocalDate dob;
        private String gender;
        private String contactInfo;
        private UUID assignedDoctorId;
        private UUID assignedNurseId;

        public UpdateRequest() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public LocalDate getDob() { return dob; }
        public void setDob(LocalDate dob) { this.dob = dob; }

        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }

        public String getContactInfo() { return contactInfo; }
        public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }

        public UUID getAssignedDoctorId() { return assignedDoctorId; }
        public void setAssignedDoctorId(UUID assignedDoctorId) { this.assignedDoctorId = assignedDoctorId; }

        public UUID getAssignedNurseId() { return assignedNurseId; }
        public void setAssignedNurseId(UUID assignedNurseId) { this.assignedNurseId = assignedNurseId; }
    }

    public static class PatientResponse {
        private UUID id;
        private String mrn;
        private String name;
        private LocalDate dob;
        private String gender;
        private String contactInfo;
        private UUID assignedDoctorId;
        private String assignedDoctorName;
        private UUID assignedNurseId;
        private String assignedNurseName;
        private LocalDateTime createdAt;

        public PatientResponse() {}

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

        public UUID getAssignedDoctorId() { return assignedDoctorId; }
        public void setAssignedDoctorId(UUID assignedDoctorId) { this.assignedDoctorId = assignedDoctorId; }

        public String getAssignedDoctorName() { return assignedDoctorName; }
        public void setAssignedDoctorName(String assignedDoctorName) { this.assignedDoctorName = assignedDoctorName; }

        public UUID getAssignedNurseId() { return assignedNurseId; }
        public void setAssignedNurseId(UUID assignedNurseId) { this.assignedNurseId = assignedNurseId; }

        public String getAssignedNurseName() { return assignedNurseName; }
        public void setAssignedNurseName(String assignedNurseName) { this.assignedNurseName = assignedNurseName; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public static PatientResponseBuilder builder() {
            return new PatientResponseBuilder();
        }

        public static class PatientResponseBuilder {
            private final PatientResponse response = new PatientResponse();

            public PatientResponseBuilder id(UUID id) { response.setId(id); return this; }
            public PatientResponseBuilder mrn(String mrn) { response.setMrn(mrn); return this; }
            public PatientResponseBuilder name(String name) { response.setName(name); return this; }
            public PatientResponseBuilder dob(LocalDate dob) { response.setDob(dob); return this; }
            public PatientResponseBuilder gender(String gender) { response.setGender(gender); return this; }
            public PatientResponseBuilder contactInfo(String contactInfo) { response.setContactInfo(contactInfo); return this; }
            public PatientResponseBuilder assignedDoctorId(UUID assignedDoctorId) { response.setAssignedDoctorId(assignedDoctorId); return this; }
            public PatientResponseBuilder assignedDoctorName(String assignedDoctorName) { response.setAssignedDoctorName(assignedDoctorName); return this; }
            public PatientResponseBuilder assignedNurseId(UUID assignedNurseId) { response.setAssignedNurseId(assignedNurseId); return this; }
            public PatientResponseBuilder assignedNurseName(String assignedNurseName) { response.setAssignedNurseName(assignedNurseName); return this; }
            public PatientResponseBuilder createdAt(LocalDateTime createdAt) { response.setCreatedAt(createdAt); return this; }

            public PatientResponse build() { return response; }
        }
    }
}
