/**
 * Realtime topic contract — MUST stay in sync with the backend
 * RealtimeTopics class. Documented in docs/adr/0001-websocket-topic-design.md.
 */
export const WS_CHANNELS = {
  /** Append-only timeline events for a patient (payload = TimelineEventResponse DTO). */
  timeline: (patientId) => `/topic/patient/${patientId}/timeline`,
  /** Live vitals stream (payload = Vital DTO). */
  vitals: (patientId) => `/topic/patient/${patientId}/vitals`,
  /** Who is currently viewing this patient (payload = { type, patientId, viewers }). */
  presence: (patientId) => `/topic/patient/${patientId}/presence`,
  /** Shift queue / task assignment changes for a nurse (payload = Task DTO). */
  nurseQueue: (nurseId) => `/topic/nurse/${nurseId}/queue`,
  /** Task state changes scoped to a patient's workspace (payload = Task DTO). */
  patientTasks: (patientId) => `/topic/patient/${patientId}/tasks`,
  /** Completed async AI shift-handoff summaries (payload = { id, patientId, aiStatus, aiSummary }). */
  handoffs: (patientId) => `/topic/patient/${patientId}/handoffs`,
  /** Private per-user notifications (payload = Notification DTO). */
  notifications: () => "/user/queue/notifications",
  /** STOMP app-destination for presence control messages (SEND). */
  presenceControl: (patientId) => `/app/presence/patient/${patientId}`,
};

export const PRESENCE_ACTIONS = {
  JOIN: "JOIN",
  LEAVE: "LEAVE",
  LIST: "LIST",
};