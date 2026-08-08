package com.careflow.ai.dto;

import com.careflow.ai.entity.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public class TimelineDto {

    public static class AppendRequest {
        @NotNull(message = "Patient ID is required")
        private UUID patientId;

        @NotNull(message = "Event type is required")
        private EventType eventType;

        @NotBlank(message = "Description is required")
        private String description;

        public AppendRequest() {}

        public UUID getPatientId() { return patientId; }
        public void setPatientId(UUID patientId) { this.patientId = patientId; }

        public EventType getEventType() { return eventType; }
        public void setEventType(EventType eventType) { this.eventType = eventType; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class CorrectionRequest {
        @NotNull(message = "Event ID to correct is required")
        private UUID correctsEventId;

        @NotBlank(message = "Correction description is required")
        private String description;

        public CorrectionRequest() {}

        public UUID getCorrectsEventId() { return correctsEventId; }
        public void setCorrectsEventId(UUID correctsEventId) { this.correctsEventId = correctsEventId; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class TimelineEventResponse {
        private UUID id;
        private UUID patientId;
        private EventType eventType;
        private String description;
        private UUID correctsEventId;
        private UUID createdById;
        private String createdByName;
        private LocalDateTime createdAt;

        public TimelineEventResponse() {}

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public UUID getPatientId() { return patientId; }
        public void setPatientId(UUID patientId) { this.patientId = patientId; }

        public EventType getEventType() { return eventType; }
        public void setEventType(EventType eventType) { this.eventType = eventType; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public UUID getCorrectsEventId() { return correctsEventId; }
        public void setCorrectsEventId(UUID correctsEventId) { this.correctsEventId = correctsEventId; }

        public UUID getCreatedById() { return createdById; }
        public void setCreatedById(UUID createdById) { this.createdById = createdById; }

        public String getCreatedByName() { return createdByName; }
        public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public static TimelineEventResponseBuilder builder() {
            return new TimelineEventResponseBuilder();
        }

        public static class TimelineEventResponseBuilder {
            private final TimelineEventResponse response = new TimelineEventResponse();

            public TimelineEventResponseBuilder id(UUID id) { response.setId(id); return this; }
            public TimelineEventResponseBuilder patientId(UUID patientId) { response.setPatientId(patientId); return this; }
            public TimelineEventResponseBuilder eventType(EventType eventType) { response.setEventType(eventType); return this; }
            public TimelineEventResponseBuilder description(String description) { response.setDescription(description); return this; }
            public TimelineEventResponseBuilder correctsEventId(UUID correctsEventId) { response.setCorrectsEventId(correctsEventId); return this; }
            public TimelineEventResponseBuilder createdById(UUID createdById) { response.setCreatedById(createdById); return this; }
            public TimelineEventResponseBuilder createdByName(String createdByName) { response.setCreatedByName(createdByName); return this; }
            public TimelineEventResponseBuilder createdAt(LocalDateTime createdAt) { response.setCreatedAt(createdAt); return this; }

            public TimelineEventResponse build() { return response; }
        }
    }
}
