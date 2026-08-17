package com.careflow.ai.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "timeline_events")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corrects_event_id")
    private TimelineEvent correctsEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public TimelineEvent() {}

    public TimelineEvent(UUID id, Patient patient, EventType eventType, String description,
                         TimelineEvent correctsEvent, User createdBy, LocalDateTime createdAt) {
        this.id = id;
        this.patient = patient;
        this.eventType = eventType;
        this.description = description;
        this.correctsEvent = correctsEvent;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TimelineEvent getCorrectsEvent() { return correctsEvent; }
    public void setCorrectsEvent(TimelineEvent correctsEvent) { this.correctsEvent = correctsEvent; }

    public UUID getCorrectsEventId() {
        return correctsEvent != null ? correctsEvent.id : null;
    }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public static TimelineEventBuilder builder() {
        return new TimelineEventBuilder();
    }

    public static class TimelineEventBuilder {
        private UUID id;
        private Patient patient;
        private EventType eventType;
        private String description;
        private TimelineEvent correctsEvent;
        private User createdBy;
        private LocalDateTime createdAt;

        TimelineEventBuilder() {}

        public TimelineEventBuilder id(UUID id) { this.id = id; return this; }
        public TimelineEventBuilder patient(Patient patient) { this.patient = patient; return this; }
        public TimelineEventBuilder eventType(EventType eventType) { this.eventType = eventType; return this; }
        public TimelineEventBuilder description(String description) { this.description = description; return this; }
        public TimelineEventBuilder correctsEvent(TimelineEvent correctsEvent) { this.correctsEvent = correctsEvent; return this; }
        public TimelineEventBuilder createdBy(User createdBy) { this.createdBy = createdBy; return this; }
        public TimelineEventBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public TimelineEvent build() {
            return new TimelineEvent(id, patient, eventType, description, correctsEvent, createdBy, createdAt);
        }
    }
}
