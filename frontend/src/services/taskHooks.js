import { useEffect, useState } from "react";
import { getPendingTasksForNurse, getTasksForNurse } from "./taskApi";

export function useNurseTasks(nurseId) {
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(Boolean(nurseId));
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!nurseId) {
      setTasks([]);
      setLoading(false);
      return;
    }

    let active = true;
    setLoading(true);
    setError(null);

    getTasksForNurse(nurseId)
      .then((data) => {
        if (active) setTasks(data || []);
      })
      .catch((err) => {
        if (active) {
          setError(err);
          setTasks([]);
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [nurseId]);

  return { tasks, loading, error };
}
