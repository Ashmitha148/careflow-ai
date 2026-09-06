import { useNavigate } from "react-router-dom";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, ClipboardList, Play, Users } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import { usePatients } from "../../services/patientHooks";
import {
  completeTask,
  getTasksForNurse,
  startTask,
} from "../../services/taskApi";
import { QK } from "../../services/queryKeys";

const statusStyles = {
  PENDING: "bg-slate-500/10 text-slate-300",
  IN_PROGRESS: "bg-amber-500/10 text-amber-400",
};

function upsertTask(list, task) {
  const next = (Array.isArray(list) ? list : []).map((item) =>
    item?.id === task?.id ? task : item,
  );
  if (!next.some((item) => item?.id === task?.id)) next.unshift(task);
  return next;
}

export default function NurseDashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { patients } = usePatients();

  const myPatients = patients.filter((p) => p.assignedNurseId === user?.id);

  // Full nurse queue backed by TanStack Query. The realtime bridge applies
  // task assignments and state changes to this cache key via setQueryData as
  // they arrive on /topic/nurse/{nurseId}/queue.
  const { data: allTasks = [], isLoading } = useQuery({
    queryKey: QK.nurseTasks(user?.id),
    queryFn: () => getTasksForNurse(user.id),
    enabled: Boolean(user?.id),
  });

  const tasks = allTasks.filter(
    (t) => t.status === "PENDING" || t.status === "IN_PROGRESS",
  );
  const loading = isLoading;

  async function handleAction(action, taskId) {
    try {
      const updated =
        action === "start"
          ? await startTask(taskId, user.id)
          : await completeTask(taskId, user.id);
      // The socket delivers the same task too; upsert is idempotent.
      queryClient.setQueryData(QK.nurseTasks(user.id), (old) =>
        upsertTask(old, updated),
      );
    } catch {
      // no-op; invalidation will converge with server state
      queryClient.invalidateQueries({ queryKey: QK.nurseTasks(user?.id) });
    }
  }

  return (
    <div className="space-y-6 pb-10">
      <div>
        <p className="text-xs font-medium text-teal-400">Nurse workspace</p>
        <h1 className="mt-1 text-xl font-semibold">
          Good to see you, {user?.fullName?.split(" ")[0] || "there"}
        </h1>
        <p className="mt-1 text-xs text-[var(--text-muted)]">
          Your pending tasks and assigned patients
        </p>
      </div>

      <section className="surface overflow-hidden rounded-2xl">
        <div className="flex items-center gap-2 border-b border-[var(--border-color)] px-5 py-4">
          <ClipboardList className="h-4 w-4 text-teal-400" />
          <h2 className="text-sm font-semibold">Pending tasks</h2>
        </div>

        {loading && (
          <div className="px-5 py-8 text-center text-sm text-[var(--text-muted)]">Loading tasks...</div>
        )}

        {!loading && tasks.length === 0 && (
          <div className="px-5 py-8 text-center text-sm text-[var(--text-muted)]">
            No pending tasks right now.
          </div>
        )}

        {!loading && tasks.length > 0 && (
          <div className="divide-y divide-[var(--border-color)]">
            {tasks.map((task) => (
              <div key={task.id} className="flex items-center justify-between gap-3 px-5 py-3">
                <div className="min-w-0">
                  <p className="truncate text-xs font-medium">{task.title}</p>
                  {task.description && (
                    <p className="mt-0.5 truncate text-[11px] text-[var(--text-muted)]">
                      {task.description}
                    </p>
                  )}
                </div>

                <div className="flex shrink-0 items-center gap-2">
                  <span className={`rounded-full px-2 py-0.5 text-[10px] font-medium ${statusStyles[task.status] || ""}`}>
                    {task.status}
                  </span>

                  {task.status === "PENDING" && (
                    <button
                      type="button"
                      onClick={() => handleAction("start", task.id)}
                      title="Start task"
                      className="rounded-lg p-1.5 text-teal-400 hover:bg-teal-500/10"
                    >
                      <Play className="h-3.5 w-3.5" />
                    </button>
                  )}

                  {task.status === "IN_PROGRESS" && (
                    <button
                      type="button"
                      onClick={() => handleAction("complete", task.id)}
                      title="Complete task"
                      className="rounded-lg p-1.5 text-emerald-400 hover:bg-emerald-500/10"
                    >
                      <CheckCircle2 className="h-3.5 w-3.5" />
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="surface overflow-hidden rounded-2xl">
        <div className="flex items-center gap-2 border-b border-[var(--border-color)] px-5 py-4">
          <Users className="h-4 w-4 text-teal-400" />
          <h2 className="text-sm font-semibold">My patients</h2>
        </div>

        {myPatients.length === 0 && (
          <div className="px-5 py-8 text-center text-sm text-[var(--text-muted)]">
            No patients assigned to you yet.
          </div>
        )}

        {myPatients.length > 0 && (
          <div className="divide-y divide-[var(--border-color)]">
            {myPatients.map((p) => (
              <button
                key={p.id}
                type="button"
                onClick={() => navigate(`/patients/${p.id}`)}
                className="flex w-full items-center justify-between px-5 py-3 text-left transition hover:bg-[var(--bg-subtle)]"
              >
                <div>
                  <p className="text-xs font-medium">{p.name}</p>
                  <p className="mt-0.5 text-[11px] text-[var(--text-muted)]">{p.mrn}</p>
                </div>
                <span className="text-[11px] text-teal-400">Open record</span>
              </button>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
