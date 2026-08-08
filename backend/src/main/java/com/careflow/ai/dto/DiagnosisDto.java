package com.careflow.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public class DiagnosisDto {

    public static class CreateRequest {
        @NotNull(message = "Patient ID is required")
        private UUID patientId;

        @NotBlank(message = "Condition is required")
        private String condition;

        private String notes;

        public CreateRequest() {}

        public UUID getPatientId() { return patientId; }
        public void setPatientId(UUID patientId) { this.patientId = patientId; }

        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    public static class DiagnosisResponse {
        private UUID id;
        private UUID patientId;
        private UUID doctorId;
        private String doctorName;
        private String condition;
        private String notes;
        private LocalDateTime diagnosedAt;
        private UUID timelineEventId;

        public DiagnosisResponse() {}

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public UUID getPatientId() { return patientId; }
        public void setPatientId(UUID patientId) { this.patientId = patientId; }

        public UUID getDoctorId() { return doctorId; }
        public void setDoctorId(UUID doctorId) { this.doctorId = doctorId; }

        public String getDoctorName() { return doctorName; }
        public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public LocalDateTime getDiagnosedAt() { return diagnosedAt; }
        public void setDiagnosedAt(LocalDateTime diagnosedAt) { this.diagnosedAt = diagnosedAt; }

        public UUID getTimelineEventId() { return timelineEventId; }
        public void setTimelineEventId(UUID timelineEventId) { this.timelineEventId = timelineEventId; }

        public static DiagnosisResponseBuilder builder() {
            return new DiagnosisResponseBuilder();
        }

        public static class DiagnosisResponseBuilder {
            private final DiagnosisResponse response = new DiagnosisResponse();

            public DiagnosisResponseBuilder id(UUID id) { response.setId(id); return this; }
            public DiagnosisResponseBuilder patientId(UUID patientId) { response.setPatientId(patientId); return this; }
            public DiagnosisResponseBuilder doctorId(UUID doctorId) { response.setDoctorId(doctorId); return this; }
            public DiagnosisResponseBuilder doctorName(String doctorName) { response.setDoctorName(doctorName); return this; }
            public DiagnosisResponseBuilder condition(String condition) { response.setCondition(condition); return this; }
            public DiagnosisResponseBuilder notes(String notes) { response.setNotes(notes); return this; }
            public DiagnosisResponseBuilder diagnosedAt(LocalDateTime diagnosedAt) { response.setDiagnosedAt(diagnosedAt); return this; }
            public DiagnosisResponseBuilder timelineEventId(UUID timelineEventId) { response.setTimelineEventId(timelineEventId); return this; }

            public DiagnosisResponse build() { return response; }
        }
    }
}
