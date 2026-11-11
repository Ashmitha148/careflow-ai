import { useEffect, useState } from "react";
import { CheckCircle2, ClipboardList, Play, Plus, X } from "lucide-react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "../../context/AuthContext";
import {
  cancelTask,
  completeTask,
  createTask,
  getTasksForPatient,
  startTask,
} from "../../services/taskApi";
import { QK } from "../../services/queryKeys";
import { MOCK_TASKS } from "../../services/mockDemoData";

const statusStyles = {
  PENDING: "bg-slate-500/10 text-slate-300 border border-slate-500/20",
  IN_PROGRESS: "bg-amber-500/10 text-amber-400 border border-amber-500/20",
  COMPLETED: "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20",
  CANCELLED: "bg-rose-500/10 text-rose-400 border border-rose-500/20",
};

export default function TaskWidget({ patientId }) {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [assignedNurseId, setAssignedNurseId] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const isDoctorOrAdmin = user?.role === "DOCTOR" || user?.role === "ADMIN";
  const isNurse = user?.role === "NURSE";

  const {
    data: rawTasks = [],
    isPending,
  } = useQuery({
    queryKey: QK.patientTasks(patientId),
    queryFn: () => getTasksForPatient(patientId),
    enabled: Boolean(patientId),
  });

  const [localTasks, setLocalTasks] = useState([]);
  const tasks = (localTasks.length > 0 ? localTasks : (rawTasks && rawTasks.length > 0 ? rawTasks : MOCK_TASKS));

  function refresh() {
    queryClient.invalidateQueries({ queryKey: QK.patientTasks(patientId) });
  }

  async function handleCreate(e) {
    e.preventDefault();
    if (!title.trim()) return;
    setSubmitting(true);
    const newTask = {
      id: "task-" + Date.now(),
      patientId,
      assignedNurseId: assignedNurseId || "33333333-3333-3333-3333-333333333333",
      assignedNurseName: "Nurse Ananya Iyer",
      title,
      description,
      status: "PENDING",
      dueAt: new Date(Date.now() + 3600000).toISOString(),
    };
    setLocalTasks((prev) => [newTask, ...prev]);

    try {
      await createTask({
        patientId,
        assignedNurseId: assignedNurseId || "33333333-3333-3333-3333-333333333333",
        title,
        description,
      });
      setTitle("");
      setDescription("");
      setAssignedNurseId("");
      setShowForm(false);
    } catch {
      setShowForm(false);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleAction(action, taskId) {
    const nextStatus = action === "start" ? "IN_PROGRESS" : action === "complete" ? "COMPLETED" : "CANCELLED";
    setLocalTasks((prev) =>
      (prev.length > 0 ? prev : tasks).map((t) => (t.id === taskId ? { ...t, status: nextStatus } : t))
    );

    try {
      if (action === "start") await startTask(taskId);
      if (action === "complete") await completeTask(taskId);
      if (action === "cancel") await cancelTask(taskId);
      refresh();
    } catch {
      // Retain optimistic state
    }
  }

  return (
    <div className="surface overflow-hidden rounded-2xl border border-[var(--border-color)]">
      <div className="flex items-center justify-between border-b border-[var(--border-color)] px-5 py-4">
        <div className="flex items-center gap-2">
          <ClipboardList className="h-4 w-4 text-teal-400" />
          <h2 className="text-sm font-bold text-[var(--text-primary)]">Assigned Care Tasks</h2>
        </div>

        {isDoctorOrAdmin && (
          <button
            type="button"
            onClick={() => setShowForm((v) => !v)}
            className="inline-flex items-center gap-1.5 rounded-lg bg-teal-500/10 px-3 py-1.5 text-xs font-semibold text-teal-300 hover:bg-teal-500/20 transition"
          >
            <Plus className="h-3.5 w-3.5" />
            <span>New Task</span>
          </button>
        )}
      </div>

      {showForm && (
        <form onSubmit={handleCreate} className="space-y-3 border-b border-[var(--border-color)] bg-[var(--bg-subtle)] p-5">
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Task title (e.g. Sternal wound dressing change)"
            required
            className="w-full rounded-xl border border-[var(--border-color)] bg-[var(--bg-sidebar)] px-3 py-2 text-xs text-[var(--text-primary)] outline-none focus:border-teal-500/50"
          />
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Clinical instructions for attending nurse..."
            rows={2}
            className="w-full rounded-xl border border-[var(--border-color)] bg-[var(--bg-sidebar)] p-3 text-xs text-[var(--text-primary)] outline-none focus:border-teal-500/50 resize-none"
          />
          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setShowForm(false)}
              className="rounded-lg px-3 py-1.5 text-xs font-medium text-[var(--text-muted)] hover:bg-[var(--bg-hover)]"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="rounded-lg bg-teal-500 px-3.5 py-1.5 text-xs font-bold text-slate-950 disabled:opacity-60 hover:bg-teal-400 transition"
            >
              {submitting ? "Creating..." : "Save Task"}
            </button>
          </div>
        </form>
      )}

      {tasks.length > 0 && (
        <div className="divide-y divide-[var(--border-color)]">
          {tasks.map((task) => (
            <div key={task.id} className="flex items-center justify-between gap-3 px-5 py-3.5 hover:bg-[var(--bg-subtle)] transition">
              <div className="min-w-0">
                <p className="truncate text-xs font-semibold text-[var(--text-primary)]">{task.title}</p>
                {task.description && (
                  <p className="mt-0.5 truncate text-[11px] text-[var(--text-secondary)]">{task.description}</p>
                )}
                <p className="mt-1 text-[10px] text-[var(--text-muted)]">
                  Assigned: {task.assignedNurseName || "Nurse Ananya Iyer"}
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
    </div>
  );
}
