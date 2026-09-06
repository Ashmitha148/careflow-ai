/**
 * Query keys shared between the TanStack Query hooks and the realtime bridge.
 * The bridge mutates exactly these keys with queryClient.setQueryData, so an
 * incoming socket message updates the same cache entry every screen reads.
 */
export const QK = {
  patient: (id) => ["patient", id],
  patientTimeline: (id, eventType) => ["patient-timeline", id, eventType || "ALL"],
  patientVitals: (id) => ["patient-vitals", id],
  patientTasks: (id) => ["patient-tasks", id],
  nurseTasks: (nurseId) => ["nurse-tasks", nurseId],
  notifications: (userId) => ["notifications", userId],
  unreadCount: (userId) => ["notifications-unread", userId],
  dashboardSummary: () => ["dashboard-summary"],
};