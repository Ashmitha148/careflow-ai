import { QK } from "../queryKeys";
import { useStore } from "../../store/useStore";

/**
 * Pure-cache mutations applied when socket messages arrive.
 *
 * Each handler updates the SAME TanStack Query cache entry that the matching
 * data hook reads — that is the entire point of the socket layer: no refetch,
 * no duplicate fetch-on-arrival. Payload shapes are the exact DTOs the REST
 * endpoints return, so no transformation is required.
 */

function upsertById(existing, incoming) {
  if (existing && !Array.isArray(existing) && Array.isArray(existing.items)) {
    const list = existing.items.filter((item) => item && item.id !== incoming?.id);
    list.unshift(incoming);
    return { ...existing, items: list };
  }
  const list = Array.isArray(existing) ? existing : [];
  const next = list.filter((item) => item && item.id !== incoming?.id);
  next.unshift(incoming);
  return next;
}

/** New append-only timeline event for a patient. */
export function handleTimelineEvent(queryClient, event) {
  if (!event?.id || !event.patientId) return;

  // Update the unfiltered feed (what the timeline screen shows by default).
  const allKey = QK.patientTimeline(event.patientId, "ALL");
  queryClient.setQueryData(allKey, (old) => upsertById(old, event));

  // If a filtered feed is cached for this patient and event type, update it too
  // (e.g. someone has the "VITAL" filter pinned while vitals stream in).
  const filteredKey = QK.patientTimeline(event.patientId, String(event.eventType));
  const filtered = queryClient.getQueryData(filteredKey);
  if (filtered !== undefined) {
    queryClient.setQueryData(filteredKey, (old) => upsertById(old, event));
  }
}

/** Live vital reading (vitals are ordered newest-first on the REST side). */
export function handleVitalEvent(queryClient, vital) {
  if (!vital?.id || !vital.patientId) return;
  if (!queryClient.getQueryData(QK.patientVitals(vital.patientId))) return;
  queryClient.setQueryData(QK.patientVitals(vital.patientId), (old) =>
    // Replace an existing reading for the same observation time/id.
    upsertById(old, vital),
  );
}

/** Task state change — reflected on both the nurse queue and the patient view. */
export function handleTaskEvent(queryClient, task) {
  if (!task?.id) return;

  const nurseId = task.assignedNurseId || task.assignedNurse?.id;
  if (nurseId) {
    queryClient.setQueryData(QK.nurseTasks(nurseId), (old) => {
      const list = Array.isArray(old) ? old : [];
      let changed = false;
      const next = list.map((item) => {
        if (item?.id === task.id) {
          changed = true;
          return task;
        }
        return item;
      });
      if (!changed) next.unshift(task);
      return next;
    });
  }

  const patientId = task.patientId || task.patient?.id;
  if (patientId) {
    queryClient.setQueryData(QK.patientTasks(patientId), (old) => {
      const list = Array.isArray(old) ? old : [];
      let changed = false;
      const next = list.map((item) => {
        if (item?.id === task.id) {
          changed = true;
          return task;
        }
        return item;
      });
      if (!changed) next.push(task);
      return next;
    });
  }
}

/** Private notification → unread cache + the store badge/toast. */
export function handleNotificationEvent(queryClient, notification, userId) {
  if (!notification?.id) return;

  queryClient.setQueryData(QK.notifications(userId), (old) => upsertById(old, notification));

  const oldCount = queryClient.getQueryData(QK.unreadCount(userId));
  if (Number.isFinite(oldCount)) {
    queryClient.setQueryData(QK.unreadCount(userId), () => (oldCount || 0) + 1);
  } else {
    queryClient.invalidateQueries({ queryKey: QK.unreadCount(userId) });
  }

  if (!notification.read) {
    const store = useStore.getState();
    useStore.setState({
      unreadNotifications: (store.unreadNotifications || 0) + 1,
      latestNotification: notification,
    });
  }
}

/** Completed async AI handoff summary pushed by the Phase 2 outbox worker. */
export function handleHandoffEvent(queryClient, handoff) {
  if (!handoff?.id || !handoff.patientId) return;
  queryClient.setQueryData(QK.patientHandoffs(handoff.patientId), (old) => {
    const list = Array.isArray(old) ? old : [];
    let replaced = false;
    const next = list.map((item) => {
      if (item?.id === handoff.id) {
        replaced = true;
        // Merge: the worker payload carries only the AI lifecycle fields.
        return { ...item, ...handoff };
      }
      return item;
    });
    return replaced ? next : [handoff, ...list];
  });
}

/** Presence snapshot from /topic/patient/{id}/presence. */
export function extractViewers(payload) {
  if (!Array.isArray(payload?.viewers)) return [];
  return payload.viewers;
}