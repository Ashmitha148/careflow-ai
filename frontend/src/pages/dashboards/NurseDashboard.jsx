import { useNavigate } from "react-router-dom";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, ClipboardList, Play, Users, ChevronRight } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import { usePatients } from "../../services/patientHooks";
import {
  completeTask,
  getTasksForNurse,
  startTask,
} from "../../services/taskApi";
import { QK } from "../../services/queryKeys";
import { MOCK_PATIENTS, MOCK_TASKS } from "../../services/mockDemoData";

const statusStyles = {
  PENDING: "bg-slate-500/10 text-slate-300 border border-slate-500/20",
  IN_PROGRESS: "bg-amber-500/10 text-amber-400 border border-amber-500/20",
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
  const { patients: hookPatients } = usePatients();

  const patients = (hookPatients && hookPatients.length > 0) ? hookPatients : MOCK_PATIENTS;
  const myPatients = patients.filter((p) => p.assignedNurseId === user?.id).length > 0
    ? patients.filter((p) => p.assignedNurseId === user?.id)
    : patients;

  const { data: allTasks = [], isLoading } = useQuery({
    queryKey: QK.nurseTasks(user?.id),
    queryFn: () => getTasksForNurse(user.id),
    enabled: Boolean(user?.id),
  });

  const availableTasks = allTasks.length > 0 ? allTasks : MOCK_TASKS;
  const tasks = availableTasks.filter(
    (t) => t.status === "PENDING" || t.status === "IN_PROGRESS",
  );
  const loading = isLoading && allTasks.length === 0;

  async function handleAction(action, taskId) {
    try {
      const updated =
        action === "start"
          ? await startTask(taskId)
          : await completeTask(taskId);
      queryClient.setQueryData(QK.nurseTasks(user.id), (old) =>
        upsertTask(old, updated),
      );
    } catch {
      queryClient.invalidateQueries({ queryKey: QK.nurseTasks(user?.id) });
    }
  }

  const nurseDisplayName = user?.fullName || "Nurse Ananya Iyer";

  return (
    <div className="space-y-6 pb-12">
      <div>
        <p className="text-xs font-semibold uppercase tracking-wider text-teal-400">Apollo CareFlow Institute · Nursing Station</p>
        <h1 className="mt-0.5 text-2xl font-bold tracking-tight text-[var(--text-primary)]">
          Welcome, {nurseDisplayName}
        </h1>
        <p className="mt-0.5 text-xs text-[var(--text-secondary)]">
          Active shift queue, scheduled medication passes, and patient monitoring
        </p>
      </div>

      <section className="surface rounded-2xl border border-[var(--border-color)] overflow-hidden">
        <div className="flex items-center gap-2 border-b border-[var(--border-color)] px-5 py-4">
          <ClipboardList className="h-4 w-4 text-teal-400" />
          <h2 className="text-sm font-bold text-[var(--text-primary)]">
            Shift Directives & Tasks ({tasks.length})
          </h2>
        </div>

        {tasks.length > 0 && (
          <div className="divide-y divide-[var(--border-color)]">
            {tasks.map((task) => (
              <div key={task.id} className="flex items-center justify-between gap-3 px-5 py-3.5 hover:bg-[var(--bg-subtle)] transition">
                <div className="min-w-0">
                  <p className="truncate text-xs font-semibold text-[var(--text-primary)]">{task.title}</p>
                  {task.description && (
                    <p className="mt-0.5 truncate text-[11px] text-[var(--text-secondary)]">
                      {task.description}
                    </p>
                  )}
                  <p className="mt-1 text-[10px] text-teal-300 font-medium">
                    Patient: {task.patientName || "Ramesh Kumar"}
                  </p>
                </div>

                <div className="flex shrink-0 items-center gap-2">
                  <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${statusStyles[task.status] || ""}`}>
                    {task.status}
                  </span>

                  {task.status === "PENDING" && (
                    <button
                      type="button"
                      onClick={() => handleAction("start", task.id)}
                      title="Start task"
                      className="rounded-lg p-1.5 text-teal-400 hover:bg-teal-500/10 border border-teal-500/20"
                    >
                      <Play className="h-3.5 w-3.5" />
                    </button>
                  )}

                  {task.status === "IN_PROGRESS" && (
                    <button
                      type="button"
                      onClick={() => handleAction("complete", task.id)}
                      title="Complete task"
                      className="rounded-lg p-1.5 text-emerald-400 hover:bg-emerald-500/10 border border-emerald-500/20"
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

      <section className="surface rounded-2xl border border-[var(--border-color)] overflow-hidden">
        <div className="flex items-center gap-2 border-b border-[var(--border-color)] px-5 py-4">
          <Users className="h-4 w-4 text-teal-400" />
          <h2 className="text-sm font-bold text-[var(--text-primary)]">
            Assigned Patients ({myPatients.length})
          </h2>
        </div>

        <div className="divide-y divide-[var(--border-color)]">
          {myPatients.map((p) => (
            <button
              key={p.id}
              type="button"
              onClick={() => navigate(`/patients/${p.id}`)}
              className="flex w-full items-center justify-between px-5 py-3.5 text-left transition hover:bg-[var(--bg-subtle)] group"
            >
              <div>
                <p className="text-xs font-semibold text-[var(--text-primary)] group-hover:text-teal-400 transition">{p.name}</p>
                <p className="mt-0.5 text-[11px] font-mono text-[var(--text-muted)]">{p.mrn} · Monitored Bed</p>
              </div>
              <div className="flex items-center gap-1 text-xs font-semibold text-teal-400">
                <span>View Chart</span>
                <ChevronRight className="h-3.5 w-3.5 group-hover:translate-x-0.5 transition" />
              </div>
            </button>
          ))}
        </div>
      </section>
    </div>
  );
}
