import { useState, useEffect } from "react";
import { ClipboardList, CheckCircle2, Play, Clock, AlertCircle, Plus, Check } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { getTasks, startTask, completeTask } from "../services/taskApi";
import { MOCK_TASKS } from "../services/mockDemoData";

const statusStyles = {
  PENDING: "bg-slate-500/10 text-slate-700 dark:text-slate-300 border border-slate-400/30 dark:border-slate-500/20",
  IN_PROGRESS: "bg-amber-500/15 text-amber-700 dark:text-amber-400 border border-amber-500/30 dark:border-amber-500/20",
  COMPLETED: "bg-emerald-500/15 text-emerald-700 dark:text-emerald-400 border border-emerald-500/30 dark:border-emerald-500/20",
  CANCELLED: "bg-rose-500/15 text-rose-700 dark:text-rose-400 border border-rose-500/30 dark:border-rose-500/20",
};

export default function TasksPage() {
  const { user } = useAuth();
  const [tasks, setTasks] = useState(MOCK_TASKS);
  const [loading, setLoading] = useState(false);
  const [filter, setFilter] = useState("ALL");

  useEffect(() => {
    getTasks()
      .then((data) => {
        if (Array.isArray(data) && data.length > 0) {
          setTasks(data);
        } else {
          setTasks(MOCK_TASKS);
        }
      })
      .catch(() => setTasks(MOCK_TASKS))
      .finally(() => setLoading(false));
  }, [user?.id]);

  const filteredTasks = filter === "ALL" ? tasks : tasks.filter((t) => t.status === filter);

  const handleAction = async (action, taskId) => {
    const nextStatus = action === "start" ? "IN_PROGRESS" : "COMPLETED";
    // Optimistic update
    setTasks((prev) =>
      prev.map((t) => (t.id === taskId ? { ...t, status: nextStatus } : t))
    );

    try {
      if (action === "start") {
        await startTask(taskId);
      } else if (action === "complete") {
        await completeTask(taskId);
      }
    } catch (_) {
      // Retain optimistic state
    }
  };

  return (
    <div className="space-y-6 pb-12">
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider text-teal-400">Clinical Workflow & Directives</p>
          <h1 className="mt-0.5 text-2xl font-bold tracking-tight text-[var(--text-primary)]">
            Clinical Care Tasks
          </h1>
          <p className="text-xs text-[var(--text-secondary)]">
            Bedside nursing directives, post-op checks, and diagnostic follow-ups
          </p>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        {["ALL", "PENDING", "IN_PROGRESS", "COMPLETED"].map((f) => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={`rounded-xl px-3 py-1.5 text-xs font-semibold transition ${
              filter === f
                ? "bg-teal-500 text-slate-950 shadow-sm"
                : "border border-[var(--border-color)] bg-[var(--bg-subtle)] text-[var(--text-secondary)] hover:bg-[var(--bg-hover)]"
            }`}
          >
            {f.replace("_", " ")}
          </button>
        ))}
      </div>

      <div className="surface rounded-2xl border border-[var(--border-color)] overflow-hidden divide-y divide-[var(--border-color)]">
        {filteredTasks.map((task) => (
          <div key={task.id} className="flex flex-col gap-3 px-5 py-4 sm:flex-row sm:items-center sm:justify-between hover:bg-[var(--bg-subtle)] transition">
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <p className="text-sm font-semibold text-[var(--text-primary)]">{task.title}</p>
                <span className={`shrink-0 rounded-full px-2 py-0.5 text-[10px] font-semibold ${statusStyles[task.status] || ""}`}>
                  {task.status}
                </span>
              </div>
              {task.description && (
                <p className="mt-0.5 text-xs text-[var(--text-secondary)]">{task.description}</p>
              )}
              <div className="mt-1 flex items-center gap-3 text-[11px] text-[var(--text-muted)]">
                <span className="font-semibold text-teal-600 dark:text-teal-300">{task.patientName || "Ramesh Kumar"}</span>
                <span>·</span>
                <span>Assigned: {task.assignedNurseName || "Nurse Ananya Iyer"}</span>
                <span>·</span>
                <span className="flex items-center gap-1 font-mono text-[10px]">
                  <Clock className="h-3 w-3" />
                  {task.dueAt ? new Date(task.dueAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : "Shift Due"}
                </span>
              </div>
            </div>

            <div className="flex items-center gap-2">
              {task.status === "PENDING" && (
                <button
                  onClick={() => handleAction("start", task.id)}
                  className="flex items-center gap-1 rounded-lg border border-[var(--border-color)] bg-[var(--bg-card)] px-3 py-1.5 text-xs font-semibold text-teal-600 dark:text-teal-400 hover:bg-teal-500/10 transition shadow-sm"
                >
                  <Play className="h-3.5 w-3.5" />
                  <span>Start Task</span>
                </button>
              )}

              {task.status === "IN_PROGRESS" && (
                <button
                  onClick={() => handleAction("complete", task.id)}
                  className="flex items-center gap-1 rounded-lg bg-teal-500 px-3 py-1.5 text-xs font-semibold text-slate-950 hover:bg-teal-400 transition shadow-sm"
                >
                  <Check className="h-3.5 w-3.5" />
                  <span>Complete Task</span>
                </button>
              )}

              {task.status === "COMPLETED" && (
                <span className="flex items-center gap-1 text-xs font-semibold text-emerald-400">
                  <CheckCircle2 className="h-4 w-4" />
                  <span>Completed</span>
                </span>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
