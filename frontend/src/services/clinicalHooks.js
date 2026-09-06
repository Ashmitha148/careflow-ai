import { useEffect, useState } from "react";
import {
  getPatientMedications,
  getPatientVitals,
} from "./clinicalApi";
import { useQuery } from "@tanstack/react-query";
import { QK } from "./queryKeys";

/**
 * Vitals backed by TanStack Query. The realtime bridge appends live readings
 * to ["patient-vitals", patientId] when they arrive on /topic/patient/{id}/vitals.
 */
export function usePatientVitals(patientId) {
  const query = useQuery({
    queryKey: QK.patientVitals(patientId),
    queryFn: () => getPatientVitals(patientId),
    enabled: Boolean(patientId),
  });

  return {
    vitals: query.data ?? [],
    loading: query.isLoading,
    error: query.error ?? null,
  };
}

export function usePatientMedications(patientId) {
  const [medications, setMedications] = useState([]);
  const [loading, setLoading] = useState(Boolean(patientId));
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!patientId) {
      setMedications([]);
      setLoading(false);
      return;
    }

    let active = true;
    setLoading(true);
    setError(null);

    getPatientMedications(patientId)
      .then((data) => {
        if (active) setMedications(data || []);
      })
      .catch((err) => {
        if (active) {
          setError(err);
          setMedications([]);
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [patientId]);

  return { medications, loading, error };
}
