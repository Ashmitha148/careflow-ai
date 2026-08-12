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
        private CriticalAlertDetail criticalAlert;

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

        public CriticalAlertDetail getCriticalAlert() { return criticalAlert; }
        public void setCriticalAlert(CriticalAlertDetail criticalAlert) { this.criticalAlert = criticalAlert; }

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
            public TimelineEventResponseBuilder criticalAlert(CriticalAlertDetail criticalAlert) { response.setCriticalAlert(criticalAlert); return this; }

            public TimelineEventResponse build() { return response; }
        }
    }

    public static class CriticalAlertDetail {
        private String vitalType;
        private String recordedValue;
        private String threshold;
        private LocalDateTime timestamp;
        private UUID recordedById;
        private String recordedByName;
        private java.util.List<NotifiedUser> notifiedUsers;

        public CriticalAlertDetail() {}

        public String getVitalType() { return vitalType; }
        public void setVitalType(String vitalType) { this.vitalType = vitalType; }

        public String getRecordedValue() { return recordedValue; }
        public void setRecordedValue(String recordedValue) { this.recordedValue = recordedValue; }

        public String getThreshold() { return threshold; }
        public void setThreshold(String threshold) { this.threshold = threshold; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public UUID getRecordedById() { return recordedById; }
        public void setRecordedById(UUID recordedById) { this.recordedById = recordedById; }

        public String getRecordedByName() { return recordedByName; }
        public void setRecordedByName(String recordedByName) { this.recordedByName = recordedByName; }

        public java.util.List<NotifiedUser> getNotifiedUsers() { return notifiedUsers; }
        public void setNotifiedUsers(java.util.List<NotifiedUser> notifiedUsers) { this.notifiedUsers = notifiedUsers; }

        public static CriticalAlertDetailBuilder builder() {
            return new CriticalAlertDetailBuilder();
        }

        public static class CriticalAlertDetailBuilder {
            private final CriticalAlertDetail detail = new CriticalAlertDetail();

            public CriticalAlertDetailBuilder vitalType(String value) { detail.setVitalType(value); return this; }
            public CriticalAlertDetailBuilder recordedValue(String value) { detail.setRecordedValue(value); return this; }
            public CriticalAlertDetailBuilder threshold(String value) { detail.setThreshold(value); return this; }
            public CriticalAlertDetailBuilder timestamp(LocalDateTime value) { detail.setTimestamp(value); return this; }
            public CriticalAlertDetailBuilder recordedById(UUID value) { detail.setRecordedById(value); return this; }
            public CriticalAlertDetailBuilder recordedByName(String value) { detail.setRecordedByName(value); return this; }
            public CriticalAlertDetailBuilder notifiedUsers(java.util.List<NotifiedUser> value) { detail.setNotifiedUsers(value); return this; }

            public CriticalAlertDetail build() { return detail; }
        }
    }

    public static class NotifiedUser {
        private UUID id;
        private String fullName;

        public NotifiedUser() {}

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public static NotifiedUserBuilder builder() {
            return new NotifiedUserBuilder();
        }

        public static class NotifiedUserBuilder {
            private final NotifiedUser user = new NotifiedUser();

            public NotifiedUserBuilder id(UUID value) { user.setId(value); return this; }
            public NotifiedUserBuilder fullName(String value) { user.setFullName(value); return this; }

            public NotifiedUser build() { return user; }
        }
    }
}
