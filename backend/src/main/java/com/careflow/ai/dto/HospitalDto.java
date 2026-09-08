package com.careflow.ai.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Phase 8 — Hospital management DTOs (facility onboarding + tenant admin).
 */
public class HospitalDto {

    public static class HospitalResponse {
        private UUID id;
        private String name;
        private String code;
        private long patientCount;
        private long staffCount;
        private LocalDateTime createdAt;

        public HospitalResponse() {}

        public HospitalResponse(UUID id, String name, String code,
                                long patientCount, long staffCount,
                                LocalDateTime createdAt) {
            this.id = id;
            this.name = name;
            this.code = code;
            this.patientCount = patientCount;
            this.staffCount = staffCount;
            this.createdAt = createdAt;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public long getPatientCount() { return patientCount; }
        public void setPatientCount(long patientCount) { this.patientCount = patientCount; }
        public long getStaffCount() { return staffCount; }
        public void setStaffCount(long staffCount) { this.staffCount = staffCount; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public static HospitalResponseBuilder builder() {
            return new HospitalResponseBuilder();
        }

        public static class HospitalResponseBuilder {
            private UUID id;
            private String name;
            private String code;
            private long patientCount;
            private long staffCount;
            private LocalDateTime createdAt;

            public HospitalResponseBuilder id(UUID id) { this.id = id; return this; }
            public HospitalResponseBuilder name(String name) { this.name = name; return this; }
            public HospitalResponseBuilder code(String code) { this.code = code; return this; }
            public HospitalResponseBuilder patientCount(long v) { this.patientCount = v; return this; }
            public HospitalResponseBuilder staffCount(long v) { this.staffCount = v; return this; }
            public HospitalResponseBuilder createdAt(LocalDateTime v) { this.createdAt = v; return this; }

            public HospitalResponse build() {
                return new HospitalResponse(id, name, code, patientCount, staffCount, createdAt);
            }
        }
    }

    public static class CreateHospitalRequest {
        @jakarta.validation.constraints.NotBlank(message = "Name is required")
        private String name;

        @jakarta.validation.constraints.NotBlank(message = "Code is required")
        private String code;

        public CreateHospitalRequest() {}

        public CreateHospitalRequest(String name, String code) {
            this.name = name;
            this.code = code;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    public static class StaffMemberResponse {
        private UUID id;
        private String email;
        private String fullName;
        private String role;
        private UUID hospitalId;
        private String hospitalName;

        public StaffMemberResponse() {}

        public StaffMemberResponse(UUID id, String email, String fullName,
                                   String role, UUID hospitalId, String hospitalName) {
            this.id = id;
            this.email = email;
            this.fullName = fullName;
            this.role = role;
            this.hospitalId = hospitalId;
            this.hospitalName = hospitalName;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public UUID getHospitalId() { return hospitalId; }
        public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }
        public String getHospitalName() { return hospitalName; }
        public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }

        public static StaffMemberResponseBuilder builder() {
            return new StaffMemberResponseBuilder();
        }

        public static class StaffMemberResponseBuilder {
            private UUID id;
            private String email;
            private String fullName;
            private String role;
            private UUID hospitalId;
            private String hospitalName;

            public StaffMemberResponseBuilder id(UUID id) { this.id = id; return this; }
            public StaffMemberResponseBuilder email(String email) { this.email = email; return this; }
            public StaffMemberResponseBuilder fullName(String fullName) { this.fullName = fullName; return this; }
            public StaffMemberResponseBuilder role(String role) { this.role = role; return this; }
            public StaffMemberResponseBuilder hospitalId(UUID hospitalId) { this.hospitalId = hospitalId; return this; }
            public StaffMemberResponseBuilder hospitalName(String hospitalName) { this.hospitalName = hospitalName; return this; }

            public StaffMemberResponse build() {
                return new StaffMemberResponse(id, email, fullName, role, hospitalId, hospitalName);
            }
        }
    }
}
