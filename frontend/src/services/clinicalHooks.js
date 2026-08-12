import { useEffect, useState } from "react";
import { getPatientMedications } from "./clinicalApi";

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
