import { useEffect, useState } from "react";
import { getPatient, searchPatients } from "../services/patientApi";
import { MOCK_PATIENTS } from "./mockDemoData";

export function usePatients(query = "") {
  const [patients, setPatients] = useState(MOCK_PATIENTS);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    let active = true;

    searchPatients(query)
      .then((data) => {
        if (active) {
          const list = Array.isArray(data)
            ? data
            : (data?.content || MOCK_PATIENTS);
          setPatients(list.length > 0 ? list : MOCK_PATIENTS);
        }
      })
      .catch((err) => {
        if (active) {
          setPatients(MOCK_PATIENTS);
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
  }, [query]);

  return { patients, loading, error: null };
}

export function usePatient(patientId) {
  const [patient, setPatient] = useState(() => {
    return MOCK_PATIENTS.find((p) => p.id === patientId) || MOCK_PATIENTS[0];
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!patientId) {
      setPatient(MOCK_PATIENTS[0]);
      return;
    }

    let active = true;

    getPatient(patientId)
      .then((data) => {
        if (active && data) {
          if (data.name) {
            setPatient(data);
          } else if (Array.isArray(data.content) && data.content.length > 0) {
            const found = data.content.find((p) => p.id === patientId);
            setPatient(found || data.content[0]);
          } else {
            const fallback = MOCK_PATIENTS.find((p) => p.id === patientId) || MOCK_PATIENTS[0];
            setPatient(fallback);
          }
        }
      })
      .catch(() => {
        if (active) {
          const fallback = MOCK_PATIENTS.find((p) => p.id === patientId) || MOCK_PATIENTS[0];
          setPatient(fallback);
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
  }, [patientId]);

  return { patient, loading, error: null };
}
