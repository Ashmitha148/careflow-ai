package com.careflow.ai.dto;

import com.careflow.ai.entity.EventType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DashboardDto {

    private DashboardDto() {}

    public static class Summary {
        private long tasksAwaitingAction;
        private long tasksCompleted;
        private long tasksTotal;
        private long upcomingAppointments;
        private long activePatients;
        private long handoffs;
        private List<ActivityItem> recentActivity = new ArrayList<>();

        public Summary() {}

        public long getTasksAwaitingAction() { return tasksAwaitingAction; }
        public void setTasksAwaitingAction(long tasksAwaitingAction) { this.tasksAwaitingAction = tasksAwaitingAction; }

        public long getTasksCompleted() { return tasksCompleted; }
        public void setTasksCompleted(long tasksCompleted) { this.tasksCompleted = tasksCompleted; }

        public long getTasksTotal() { return tasksTotal; }
        public void setTasksTotal(long tasksTotal) { this.tasksTotal = tasksTotal; }

        public long getUpcomingAppointments() { return upcomingAppointments; }
        public void setUpcomingAppointments(long upcomingAppointments) { this.upcomingAppointments = upcomingAppointments; }

        public long getActivePatients() { return activePatients; }
        public void setActivePatients(long activePatients) { this.activePatients = activePatients; }

        public long getHandoffs() { return handoffs; }
        public void setHandoffs(long handoffs) { this.handoffs = handoffs; }

        public List<ActivityItem> getRecentActivity() { return recentActivity; }
        public void setRecentActivity(List<ActivityItem> recentActivity) { this.recentActivity = recentActivity; }

        public static SummaryBuilder builder() {
            return new SummaryBuilder();
        }

        public static class SummaryBuilder {
            private final Summary summary = new Summary();

            public SummaryBuilder tasksAwaitingAction(long value) { summary.setTasksAwaitingAction(value); return this; }
            public SummaryBuilder tasksCompleted(long value) { summary.setTasksCompleted(value); return this; }
            public SummaryBuilder tasksTotal(long value) { summary.setTasksTotal(value); return this; }
            public SummaryBuilder upcomingAppointments(long value) { summary.setUpcomingAppointments(value); return this; }
            public SummaryBuilder activePatients(long value) { summary.setActivePatients(value); return this; }
            public SummaryBuilder handoffs(long value) { summary.setHandoffs(value); return this; }
            public SummaryBuilder recentActivity(List<ActivityItem> value) { summary.setRecentActivity(value); return this; }

            public Summary build() { return summary; }
        }
    }

    public static class ActivityItem {
        private UUID id;
        private UUID patientId;
        private String patientName;
        private EventType eventType;
        private String description;
        private LocalDateTime createdAt;

        public ActivityItem() {}

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public UUID getPatientId() { return patientId; }
        public void setPatientId(UUID patientId) { this.patientId = patientId; }

        public String getPatientName() { return patientName; }
        public void setPatientName(String patientName) { this.patientName = patientName; }

        public EventType getEventType() { return eventType; }
        public void setEventType(EventType eventType) { this.eventType = eventType; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public static ActivityItemBuilder builder() {
            return new ActivityItemBuilder();
        }

        public static class ActivityItemBuilder {
            private final ActivityItem item = new ActivityItem();

            public ActivityItemBuilder id(UUID id) { item.setId(id); return this; }
            public ActivityItemBuilder patientId(UUID patientId) { item.setPatientId(patientId); return this; }
            public ActivityItemBuilder patientName(String patientName) { item.setPatientName(patientName); return this; }
            public ActivityItemBuilder eventType(EventType eventType) { item.setEventType(eventType); return this; }
            public ActivityItemBuilder description(String description) { item.setDescription(description); return this; }
            public ActivityItemBuilder createdAt(LocalDateTime createdAt) { item.setCreatedAt(createdAt); return this; }

            public ActivityItem build() { return item; }
        }
    }
}
