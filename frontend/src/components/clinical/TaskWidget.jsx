import { useEffect, useState } from "react";
import { CheckCircle2, ClipboardList, Play, Plus, X } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import {
  cancelTask,
  completeTask,
  createTask,
  getTasksForPatient,
  startTask,
} from "../../services/taskApi";

const statusStyles = {
  PENDING: "bg-slate-500/10 text-slate-300",
  IN_PROGRESS: "bg-amber-500/10 text-amber-400",
  COMPLETED: "bg-emerald-500/10 text-emerald-400",
  CANCELLED: "bg-rose-500/10 text-rose-400",
};

export default function TaskWidget({ patientId }) {
  const { user } = useAuth();
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [assignedNurseId, setAssignedNurseId] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const isDoctorOrAdmin = user?.role === "DOCTOR" || user?.role === "ADMIN";
  const isNurse = user?.role === "NURSE";

  function refresh() {
    if (!patientId) return;
    setLoading(true);
    setError(null);
    getTasksForPatient(patientId)
      .then((data) => setTasks(data || []))
      .catch((err) => setError(err))
      .finally(() => setLoading(false));
  }

  useEffect(refresh, [patientId]);

  async function handleCreate(e) {
    e.preventDefault();
    if (!title.trim() || !assignedNurseId.trim()) return;
    setSubmitting(true);
    try {
      await createTask({
        patientId,
        doctorId: user.id,
        assignedNurseId,
        title,
        description,
      });
      setTitle("");
      setDescription("");
      setAssignedNurseId("");
      setShowForm(false);
      refresh();
    } catch {
      setError({ message: "Could not create task" });
    } finally {
      setSubmitting(false);
    }
  }

  async function handleAction(action, taskId) {
    try {
      if (action === "start") await startTask(taskId, user.id);
      if (action === "complete") await completeTask(taskId, user.id);
      if (action === "cancel") await cancelTask(taskId, user.id);
      refresh();
    } catch {
      setError({ message: "Action failed" });
    }
  }

  return (
    <div className="surface overflow-hidden rounded-2xl">
      <div className="flex items-center justify-between border-b border-[var(--border-color)] px-5 py-4">
        <div className="flex items-center gap-2">
          <ClipboardList className="h-4 w-4 text-teal-400" />
          <h2 className="text-sm font-semibold">Tasks</h2>
        </div>

        {isDoctorOrAdmin && (
          <button
            type="button"
            onClick={() => setShowForm((v) => !v)}
            className="inline-flex items-center gap-1.5 rounded-lg bg-teal-500/10 px-3 py-1.5 text-xs font-medium text-teal-400 hover:bg-teal-500/20"
          >
            <Plus className="h-3.5 w-3.5" />
            New task
          </button>
        )}
      </div>

      {showForm && (
        <form onSubmit={handleCreate} className="space-y-3 border-b border-[var(--border-color)] px-5 py-4">
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Task title"
            required
            className="w-full rounded-lg border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3 py-2 text-xs outline-none focus:border-teal-500/50"
          />
          <input
            value={assignedNurseId}
            onChange={(e) => setAssignedNurseId(e.target.value)}
            placeholder="Assigned nurse ID"
            required
            className="w-full rounded-lg border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3 py-2 text-xs outline-none focus:border-teal-500/50"
          />
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Description (optional)"
            rows={2}
            className="w-full rounded-lg border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3 py-2 text-xs outline-none focus:border-teal-500/50"
          />
          <button
            type="submit"
            disabled={submitting}
            className="rounded-lg bg-teal-500 px-3 py-1.5 text-xs font-semibold text-slate-950 disabled:opacity-60"
          >
            {submitting ? "Creating..." : "Create task"}
          </button>
        </form>
      )}

      {loading && (
        <div className="px-5 py-8 text-center text-sm text-[var(--text-muted)]">Loading tasks...</div>
      )}

      {error && !loading && (
        <div className="px-5 py-3 text-center text-xs text-rose-400">{error.message || "Unable to load tasks."}</div>
      )}

      {!loading && tasks.length === 0 && (
        <div className="px-5 py-8 text-center text-sm text-[var(--text-muted)]">No tasks for this patient yet.</div>
      )}

      {!loading && tasks.length > 0 && (
        <div className="divide-y divide-[var(--border-color)]">
          {tasks.map((task) => (
            <div key={task.id} className="flex items-center justify-between gap-3 px-5 py-3">
              <div className="min-w-0">
                <p className="truncate text-xs font-medium">{task.title}</p>
                {task.description && (
                  <p className="mt-0.5 truncate text-[11px] text-[var(--text-muted)]">{task.description}</p>
                )}
              </div>

              <div className="flex shrink-0 items-center gap-2">
                <span className={`rounded-full px-2 py-0.5 text-[10px] font-medium ${statusStyles[task.status] || ""}`}>
                  {task.status}
                </span>

                {isNurse && task.status === "PENDING" && (
                  <button
                    type="button"
                    onClick={() => handleAction("start", task.id)}
                    title="Start task"
                    className="rounded-lg p-1.5 text-teal-400 hover:bg-teal-500/10"
                  >
                    <Play className="h-3.5 w-3.5" />
                  </button>
                )}

                {isNurse && task.status === "IN_PROGRESS" && (
                  <button
                    type="button"
                    onClick={() => handleAction("complete", task.id)}
                    title="Complete task"
                    className="rounded-lg p-1.5 text-emerald-400 hover:bg-emerald-500/10"
                  >
                    <CheckCircle2 className="h-3.5 w-3.5" />
                  </button>
                )}

                {isDoctorOrAdmin && (task.status === "PENDING" || task.status === "IN_PROGRESS") && (
                  <button
                    type="button"
                    onClick={() => handleAction("cancel", task.id)}
                    title="Cancel task"
                    className="rounded-lg p-1.5 text-rose-400 hover:bg-rose-500/10"
                  >
                    <X className="h-3.5 w-3.5" />
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
