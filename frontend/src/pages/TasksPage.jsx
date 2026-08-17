import { useState, useEffect } from "react";
import { ClipboardList, CheckCircle2, Play, Clock, AlertCircle } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { getPendingTasksForNurse, startTask, completeTask } from "../services/taskApi";

const statusStyles = {
  PENDING: "bg-slate-500/10 text-slate-300",
  IN_PROGRESS: "bg-amber-500/10 text-amber-400",
  COMPLETED: "bg-emerald-500/10 text-emerald-400",
  CANCELLED: "bg-rose-500/10 text-rose-400",
};

export default function TasksPage() {
  const { user } = useAuth();
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState("ALL");

  useEffect(() => {
    if (!user?.id) return;
    getPendingTasksForNurse(user.id)
      .then((data) => setTasks(data || []))
      .catch(() => setTasks([]))
      .finally(() => setLoading(false));
  }, [user?.id]);

  const filteredTasks = filter === "ALL" ? tasks : tasks.filter((t) => t.status === filter);

  const handleAction = async (action, taskId) => {
    try {
      if (action === "start") {
        await startTask(taskId, user.id);
      } else if (action === "complete") {
        await completeTask(taskId, user.id);
      }
      // Refresh
      const data = await getPendingTasksForNurse(user.id);
      setTasks(data || []);
    } catch {
      // no-op
    }
  };

  return (
    <div className="space-y-6 pb-10">
      <div>
        <p className="text-xs font-medium text-teal-400">Workflow</p>
        <h1 className="mt-1 text-xl font-semibold">Tasks</h1>
        <p className="mt-1 text-xs text-[var(--text-muted)]">
          Pending and completed care tasks
        </p>
      </div>

      <div className="flex gap-2">
        {["ALL", "PENDING", "IN_PROGRESS", "COMPLETED"].map((f) => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={`rounded-lg px-3 py-1.5 text-[11px] font-medium transition ${
              filter === f
                ? "bg-teal-500/10 text-teal-400"
                : "text-[var(--text-muted)] hover:bg-[var(--bg-hover)]"
            }`}
          >
            {f.replace("_", " ")}
          </button>
        ))}
      </div>

      {loading && (
        <div className="surface rounded-2xl p-8 text-center text-sm text-[var(--text-muted)]">
          Loading tasks...
        </div>
      )}

      {!loading && filteredTasks.length === 0 && (
        <div className="surface rounded-2xl p-8 text-center">
          <ClipboardList className="mx-auto h-8 w-8 text-[var(--text-muted)]" />
          <p className="mt-3 text-sm font-medium">No tasks found</p>
          <p className="mt-1 text-xs text-[var(--text-muted)]">All caught up!</p>
        </div>
      )}

      {!loading && filteredTasks.length > 0 && (
        <div className="divide-y divide-[var(--border-color)] surface rounded-2xl overflow-hidden">
          {filteredTasks.map((task) => (
            <div key={task.id} className="flex items-center justify-between gap-3 px-5 py-4">
              <div className="min-w-0">
                <p className="text-sm font-medium">{task.title}</p>
                {task.description && (
                  <p className="mt-0.5 text-xs text-[var(--text-muted)]">{task.description}</p>
                )}
                <p className="mt-1 text-[10px] text-[var(--text-muted)] flex items-center gap-1">
                  <Clock className="h-3 w-3" />
                  {task.dueAt ? new Date(task.dueAt).toLocaleDateString() : "No due date"}
                </p>
              </div>

              <div className="flex items-center gap-2">
                <span className={`shrink-0 rounded-full px-2.5 py-1 text-[10px] font-medium ${statusStyles[task.status] || ""}`}>
                  {task.status}
                </span>

                {task.status === "PENDING" && (
                  <button
                    onClick={() => handleAction("start", task.id)}
                    className="rounded-lg p-1.5 text-teal-400 hover:bg-teal-500/10"
                    title="Start task"
                  >
                    <Play className="h-3.5 w-3.5" />
                  </button>
                )}

                {task.status === "IN_PROGRESS" && (
                  <button
                    onClick={() => handleAction("complete", task.id)}
                    className="rounded-lg p-1.5 text-emerald-400 hover:bg-emerald-500/10"
                    title="Complete task"
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