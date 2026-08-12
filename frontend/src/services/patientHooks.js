import { useEffect, useState } from "react";
import { getPatient, searchPatients } from "../services/patientApi";

export function usePatients(query = "") {
  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let active = true;

    setLoading(true);
    setError(null);

    searchPatients(query)
      .then((data) => {
        if (active) {
          setPatients(data.content || []);
        }
      })
      .catch((err) => {
        if (active) {
          setError(err);
          setPatients([]);
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

  return { patients, loading, error };
}

export function usePatient(patientId) {
  const [patient, setPatient] = useState(null);
  const [loading, setLoading] = useState(Boolean(patientId));
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!patientId) {
      setPatient(null);
      setLoading(false);
      return;
    }

    let active = true;

    setLoading(true);
    setError(null);

    getPatient(patientId)
      .then((data) => {
        if (active) {
          setPatient(data);
        }
      })
      .catch((err) => {
        if (active) {
          setError(err);
          setPatient(null);
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

  return { patient, loading, error };
}
