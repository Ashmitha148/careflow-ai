import { useEffect, useState } from "react";
import { getPatientTimeline, getTimelineReplay } from "./timelineApi";

export function usePatientTimeline(patientId, params = {}) {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(Boolean(patientId));
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!patientId) {
      setEvents([]);
      setLoading(false);
      return;
    }

    let active = true;

    setLoading(true);
    setError(null);

    getPatientTimeline(patientId, params)
      .then((data) => {
        if (active) {
          setEvents(data || []);
        }
      })
      .catch((err) => {
        if (active) {
          setError(err);
          setEvents([]);
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [patientId, params.eventType, params.start, params.end]);

  return { events, loading, error };
}

export async function loadTimelineReplay(patientId, range) {
  return getTimelineReplay(patientId, range);
}
