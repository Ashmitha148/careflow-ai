import { useQuery } from "@tanstack/react-query";
import { getPatientTimeline, getTimelineReplay } from "./timelineApi";
import { QK } from "./queryKeys";

/**
 * Patient timeline backed by TanStack Query. The realtime bridge mutates the
 * exact same cache key (["patient-timeline", patientId, type]) via
 * queryClient.setQueryData when a new append-only event arrives on
 * /topic/patient/{id}/timeline, so this hook re-renders with the new event
 * without any refetch.
 */
export function usePatientTimeline(patientId, params = {}) {
  const query = useQuery({
    queryKey: QK.patientTimeline(patientId, params.eventType || "ALL"),
    queryFn: () => getPatientTimeline(patientId, params),
    enabled: Boolean(patientId),
  });

  return {
    events: query.data ?? [],
    loading: query.isLoading,
    error: query.error ?? null,
  };
}

export async function loadTimelineReplay(patientId, range) {
  return getTimelineReplay(patientId, range);
}
