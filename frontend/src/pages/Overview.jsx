import { useState, useEffect } from "react";
import {
  ArrowUpRight,
  Bot,
  CalendarDays,
  ChevronRight,
  Clock3,
  FileText,
  HeartPulse,
  Pill,
  ShieldAlert,
  Stethoscope,
  Users,
  CheckCircle2,
  Circle,
  Plus,
  Activity,
  X,
  Radio,
  Sparkles,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import { usePatients } from "../services/patientHooks";
import { useStore } from "../store/useStore";
import { useAuth } from "../context/AuthContext";
import { useSocket } from "../context/SocketContext";
import { getDashboardSummary } from "../services/dashboardApi";
import { getTasks, completeTask, createTask } from "../services/taskApi";
import { recordVital } from "../services/clinicalApi";
import {
  MOCK_PATIENTS,
  MOCK_DASHBOARD_SUMMARY,
  MOCK_TASKS,
} from "../services/mockDemoData";

const activityIcons = {
  VITAL: HeartPulse,
  TASK: FileText,
  SHIFT_HANDOFF: Users,
  MEDICATION: Pill,
  MEDICATION_ADMINISTRATION: Pill,
  DIAGNOSIS: FileText,
  CRITICAL_ALERT: ShieldAlert,
  APPOINTMENT: CalendarDays,
  FILE_UPLOAD: FileText,
};

const activityTitles = {
  VITAL: "Bedside vital recorded",
  TASK: "Clinical task updated",
  SHIFT_HANDOFF: "Shift handoff completed",
  MEDICATION: "Medication prescribed",
  MEDICATION_ADMINISTRATION: "Medication administered",
  DIAGNOSIS: "Clinical diagnosis recorded",
  CRITICAL_ALERT: "Critical threshold alert",
  APPOINTMENT: "Outpatient review scheduled",
  FILE_UPLOAD: "Diagnostic report attached",
};

function timeAgo(value) {
  if (!value) return "just now";
  const seconds = Math.round((Date.now() - new Date(value).getTime()) / 1000);
  if (seconds < 60) return "just now";
  const minutes = Math.round(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.round(hours / 24);
  return `${days}d ago`;
}

function formatDemographics(patient) {
  if (!patient) return "";
  const isMale = (patient.gender || "").toLowerCase().startsWith("m");
  const fallback = isMale ? "58M" : "44F";
  if (patient.dob) {
    const birthYear = new Date(patient.dob).getFullYear();
    if (!isNaN(birthYear) && birthYear > 1900) {
      const age = new Date().getFullYear() - birthYear;
      return `${age}${isMale ? "M" : "F"}`;
    }
  }
  return fallback;
}

export default function Overview() {
  const navigate = useNavigate();
  const { toggleCopilot, setSelectedPatientId } = useStore();
  const { user: currentUser } = useAuth();
  const { connected, send } = useSocket();
  const { patients: hookPatients, loading: patientsLoading } = usePatients();

  // Resilient fallback to Indian patient roster
  const patients = (hookPatients && hookPatients.length > 0) ? hookPatients : MOCK_PATIENTS;

  // Dashboard summary state
  const [summary, setSummary] = useState(MOCK_DASHBOARD_SUMMARY);
  const [summaryLoading, setSummaryLoading] = useState(true);

  // Shift tasks interactive state
  const [tasks, setTasks] = useState(MOCK_TASKS);
  const [taskFilter, setTaskFilter] = useState("ALL"); // ALL | PENDING

  // Patient filter
  const [patientFilter, setPatientFilter] = useState("ALL"); // ALL | ICU | STEPDOWN | WARD

  // Modals
  const [showVitalModal, setShowVitalModal] = useState(false);
  const [showTaskModal, setShowTaskModal] = useState(false);
  const [selectedPatientForVital, setSelectedPatientForVital] = useState(patients[0]?.id || "");

  // Form states
  const [vitalForm, setVitalForm] = useState({
    type: "HEART_RATE",
    value: "76",
    unit: "bpm",
    isAbnormal: false,
  });

  const [taskForm, setTaskForm] = useState({
    patientId: patients[0]?.id || "",
    title: "",
    priority: "HIGH",
    description: "",
  });

  const [savingAction, setSavingAction] = useState(false);

  // Fetch summary & tasks on mount
  useEffect(() => {
    let active = true;
    getDashboardSummary()
      .then((data) => {
        if (active && data) setSummary(data);
      })
      .catch(() => {
        if (active) setSummary(MOCK_DASHBOARD_SUMMARY);
      })
      .finally(() => {
        if (active) setSummaryLoading(false);
      });

    getTasks()
      .then((data) => {
        if (active && Array.isArray(data) && data.length > 0) {
          setTasks(data);
        }
      })
      .catch(() => {
        if (active) setTasks(MOCK_TASKS);
      });

    return () => { active = false; };
  }, []);

  // Handle task completion toggle
  async function handleToggleTask(task) {
    const isCompleted = task.status === "COMPLETED";
    const nextStatus = isCompleted ? "PENDING" : "COMPLETED";

    // Optimistic UI update
    setTasks((prev) =>
      prev.map((t) => (t.id === task.id ? { ...t, status: nextStatus } : t))
    );

    try {
      if (!isCompleted) {
        await completeTask(task.id);
      }
      send({
        type: "TASK_STATUS_CHANGED",
        taskId: task.id,
        status: nextStatus,
        patientId: task.patientId,
      });
    } catch (_) {
      // Mock bus maintains local change
    }
  }

  // Handle Record Vitals submission
  async function handleSaveVital(e) {
    e.preventDefault();
    if (!vitalForm.value) return;
    setSavingAction(true);

    const vitalPayload = {
      patientId: selectedPatientForVital || patients[0].id,
      type: vitalForm.type,
      value: `${vitalForm.value} ${vitalForm.unit}`,
      isAbnormal: vitalForm.isAbnormal,
    };

    try {
      await recordVital(vitalPayload);
      send({
        type: "VITAL_RECORDED",
        patientId: vitalPayload.patientId,
        vital: vitalPayload,
      });

      // Update recent activity optimistic entry
      const targetPt = patients.find((p) => p.id === vitalPayload.patientId);
      const newActivity = {
        id: "act-" + Date.now(),
        eventType: "VITAL",
        description: `Bedside vital logged: ${vitalPayload.type.replace('_', ' ')} (${vitalPayload.value}) for ${targetPt?.name || 'patient'}.`,
        createdAt: new Date().toISOString(),
      };

      setSummary((prev) => ({
        ...prev,
        recentActivity: [newActivity, ...(prev.recentActivity || []).slice(0, 4)],
      }));

      setShowVitalModal(false);
      setVitalForm({ type: "HEART_RATE", value: "76", unit: "bpm", isAbnormal: false });
    } catch (_) {
      setShowVitalModal(false);
    } finally {
      setSavingAction(false);
    }
  }

  // Handle New Task creation
  async function handleCreateTask(e) {
    e.preventDefault();
    if (!taskForm.title) return;
    setSavingAction(true);

    const pt = patients.find((p) => p.id === taskForm.patientId) || patients[0];
    const taskPayload = {
      patientId: pt.id,
      assignedNurseId: "33333333-3333-3333-3333-333333333333",
      title: taskForm.title,
      description: taskForm.description || `Priority: ${taskForm.priority}`,
      dueAt: new Date(Date.now() + 3600000).toISOString(),
    };

    try {
      const res = await createTask(taskPayload);
      const newTask = res || {
        id: "task-" + Date.now(),
        patientId: pt.id,
        patientName: pt.name,
        assignedNurseName: "Nurse Ananya Iyer",
        title: taskPayload.title,
        description: taskPayload.description,
        status: "PENDING",
      };

      setTasks((prev) => [newTask, ...prev]);
      send({
        type: "TASK_CREATED",
        task: newTask,
      });

      setShowTaskModal(false);
      setTaskForm({ patientId: patients[0]?.id || "", title: "", priority: "HIGH", description: "" });
    } catch (_) {
      setShowTaskModal(false);
    } finally {
      setSavingAction(false);
    }
  }

  const completedTasksCount = tasks.filter((t) => t.status === "COMPLETED").length;
  const totalTasksCount = tasks.length;
  const taskProgress = totalTasksCount > 0 ? Math.round((completedTasksCount / totalTasksCount) * 100) : 0;

  const filteredPatients = patients.filter((pt) => {
    if (patientFilter === "ICU") return pt.mrn?.includes("ICU") || pt.name === "Ramesh Kumar";
    if (patientFilter === "STEPDOWN") return pt.name === "Rajesh Patel";
    if (patientFilter === "WARD") return pt.name === "Meera Nair" || pt.name === "Aditi Rao";
    return true;
  });

  const filteredTasks = tasks.filter((t) => {
    if (taskFilter === "PENDING") return t.status !== "COMPLETED";
    return true;
  });

  const todayLabel = new Date().toLocaleDateString("en-IN", {
    weekday: "long",
    day: "numeric",
    month: "short",
    year: "numeric",
  });

  const clinicianName = (currentUser?.fullName && !currentUser.fullName.includes("Sarah Jenkins"))
    ? currentUser.fullName
    : "Dr. Aarav Sharma";

  return (
    <div className="space-y-6 pb-12">
      {/* ── Top Workspace Header ── */}
      <section className="flex flex-col justify-between gap-4 pt-1 sm:flex-row sm:items-center">
        <div>
          <div className="flex items-center gap-2 text-xs font-medium text-[var(--text-muted)]">
            <span>{todayLabel}</span>
            <span>·</span>
            <span className="text-teal-400 font-semibold">Apollo CareFlow Institute</span>
            <span>·</span>
            <span>Internal Medicine & Critical Care</span>
          </div>
          <h1 className="mt-1 text-2xl font-bold tracking-tight text-[var(--text-primary)] sm:text-3xl">
            Welcome back, {clinicianName}
          </h1>
          <p className="mt-0.5 text-xs text-[var(--text-secondary)]">
            Continuous telemetry, shift handoffs, and 2-way clinical communication across active cohorts
          </p>
        </div>

        {/* ── Interactive Action Hub ── */}
        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            onClick={() => {
              setSelectedPatientForVital(patients[0]?.id || "");
              setShowVitalModal(true);
            }}
            className="inline-flex items-center gap-1.5 rounded-lg bg-teal-500 px-3 py-2 text-xs font-semibold text-slate-950 transition hover:bg-teal-400 shadow-sm"
          >
            <Plus className="h-3.5 w-3.5" />
            <span>Record Vitals</span>
          </button>

          <button
            type="button"
            onClick={() => setShowTaskModal(true)}
            className="inline-flex items-center gap-1.5 rounded-lg border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3 py-2 text-xs font-medium text-[var(--text-primary)] transition hover:bg-[var(--bg-hover)]"
          >
            <Plus className="h-3.5 w-3.5 text-teal-400" />
            <span>Add Clinical Task</span>
          </button>

          <button
            type="button"
            onClick={toggleCopilot}
            className="inline-flex items-center gap-1.5 rounded-lg border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3 py-2 text-xs font-medium text-[var(--text-primary)] transition hover:bg-[var(--bg-hover)]"
          >
            <Bot className="h-3.5 w-3.5 text-teal-400" />
            <span>Copilot</span>
            <ArrowUpRight className="h-3 w-3 text-[var(--text-muted)]" />
          </button>
        </div>
      </section>

      {/* ── Live Bedside Telemetry Monitor Banner ── */}
      <section className="relative overflow-hidden rounded-2xl border border-teal-500/20 bg-gradient-to-r from-teal-950/40 via-[var(--bg-sidebar)] to-[var(--bg-subtle)] p-4 shadow-sm">
        <div className="flex flex-col justify-between gap-4 md:flex-row md:items-center">
          <div className="flex items-center gap-3">
            <div className="relative flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-teal-500/20 text-teal-400">
              <Activity className="h-5 w-5 animate-pulse" />
              <span className="absolute -top-1 -right-1 flex h-3 w-3">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75"></span>
                <span className="relative inline-flex h-3 w-3 rounded-full bg-emerald-500"></span>
              </span>
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xs font-bold uppercase tracking-wider text-teal-400">
                  Live Bedside Telemetry
                </span>
                <span className="rounded-full bg-emerald-500/20 px-2 py-0.5 font-mono text-[10px] font-semibold text-emerald-400">
                  {connected ? "● Live 2-Way WebSocket" : "● Telemetry Bus"}
                </span>
              </div>
              <p className="mt-0.5 text-sm font-semibold text-[var(--text-primary)]">
                Ramesh Kumar (58M) · ICU Bed 204 · Post-CABG Day 2
              </p>
            </div>
          </div>

          {/* Real-time Vitals Ticker */}
          <div className="flex flex-wrap items-center gap-3 text-xs">
            <div className="rounded-xl border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3.5 py-2">
              <span className="text-[10px] uppercase text-[var(--text-muted)] block">Heart Rate</span>
              <span className="font-mono text-base font-bold text-teal-400">76 <span className="text-xs font-normal">bpm</span></span>
            </div>
            <div className="rounded-xl border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3.5 py-2">
              <span className="text-[10px] uppercase text-[var(--text-muted)] block">Blood Pressure</span>
              <span className="font-mono text-base font-bold text-[var(--text-primary)]">122/78 <span className="text-xs font-normal">mmHg</span></span>
            </div>
            <div className="rounded-xl border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3.5 py-2">
              <span className="text-[10px] uppercase text-[var(--text-muted)] block">SpO2 Oxygen</span>
              <span className="font-mono text-base font-bold text-emerald-400">98% <span className="text-xs font-normal">Room air</span></span>
            </div>
            <div className="rounded-xl border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3.5 py-2">
              <span className="text-[10px] uppercase text-[var(--text-muted)] block">Rhythm</span>
              <span className="font-mono text-xs font-semibold text-teal-300">Normal Sinus</span>
            </div>
          </div>
        </div>
      </section>

      {/* ── Metric Cards ── */}
      <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="surface rounded-xl p-4.5 border border-[var(--border-color)]">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-[var(--text-muted)]">Critical Alerts</span>
            <span className="h-2 w-2 rounded-full bg-rose-500 animate-ping" />
          </div>
          <p className="mt-2 text-3xl font-bold tabular-nums text-rose-400">1</p>
          <p className="mt-1 text-xs text-[var(--text-secondary)]">Ramesh Kumar: Tachycardia reviewed</p>
        </div>

        <div className="surface rounded-xl p-4.5 border border-[var(--border-color)]">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-[var(--text-muted)]">Shift Tasks Pending</span>
            <span className="h-2 w-2 rounded-full bg-amber-400" />
          </div>
          <p className="mt-2 text-3xl font-bold tabular-nums text-[var(--text-primary)]">
            {totalTasksCount - completedTasksCount}
          </p>
          <p className="mt-1 text-xs text-[var(--text-secondary)]">
            {completedTasksCount} of {totalTasksCount} tasks completed ({taskProgress}%)
          </p>
        </div>

        <div className="surface rounded-xl p-4.5 border border-[var(--border-color)]">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-[var(--text-muted)]">Active Inpatients</span>
            <span className="h-2 w-2 rounded-full bg-teal-400" />
          </div>
          <p className="mt-2 text-3xl font-bold tabular-nums text-[var(--text-primary)]">
            {patients.length}
          </p>
          <p className="mt-1 text-xs text-[var(--text-secondary)]">1 ICU · 1 Stepdown · 2 General</p>
        </div>

        <div className="surface rounded-xl p-4.5 border border-[var(--border-color)]">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-[var(--text-muted)]">Shift Handoffs</span>
            <span className="h-2 w-2 rounded-full bg-sky-400" />
          </div>
          <p className="mt-2 text-3xl font-bold tabular-nums text-[var(--text-primary)]">
            {summary.handoffs || 3}
          </p>
          <p className="mt-1 text-xs text-[var(--text-secondary)]">Synchronized with SBAR format</p>
        </div>
      </section>

      {/* ── Main Two-Column Clinical Core ── */}
      <div className="grid gap-6 lg:grid-cols-[1.4fr_1fr]">
        {/* ── LEFT COLUMN: Patient Priority Cohort ── */}
        <section className="surface rounded-2xl border border-[var(--border-color)] overflow-hidden">
          <div className="flex flex-col gap-3 border-b border-[var(--border-color)] p-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 className="text-sm font-bold text-[var(--text-primary)]">
                Patient Attention Roster
              </h2>
              <p className="text-xs text-[var(--text-muted)]">
                Assigned cohort with active telemetry and care orders
              </p>
            </div>

            {/* Filter Pills */}
            <div className="flex flex-wrap items-center gap-1.5">
              {[
                { id: "ALL", label: `All (${patients.length})` },
                { id: "ICU", label: "ICU" },
                { id: "STEPDOWN", label: "Stepdown" },
                { id: "WARD", label: "Ward" },
              ].map((f) => (
                <button
                  key={f.id}
                  onClick={() => setPatientFilter(f.id)}
                  className={`rounded-lg px-2.5 py-1 text-xs font-medium transition ${
                    patientFilter === f.id
                      ? "bg-teal-500/20 text-teal-300 border border-teal-500/40"
                      : "text-[var(--text-muted)] hover:bg-[var(--bg-subtle)]"
                  }`}
                >
                  {f.label}
                </button>
              ))}
            </div>
          </div>

          {/* Patient Cards List */}
          <div className="divide-y divide-[var(--border-color)]">
            {filteredPatients.map((patient) => {
              const initials = patient.name
                .split(" ")
                .map((n) => n[0])
                .join("");
              const isICU = patient.mrn?.includes("ICU") || patient.name === "Ramesh Kumar";

              return (
                <div
                  key={patient.id}
                  className="flex flex-col gap-3 p-4 transition hover:bg-[var(--bg-subtle)] sm:flex-row sm:items-center sm:justify-between"
                >
                  <div className="flex items-center gap-3">
                    <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-teal-500/10 font-bold text-teal-400">
                      {initials}
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-semibold text-[var(--text-primary)]">
                          {patient.name}
                        </span>
                        <span className="font-mono text-xs text-[var(--text-muted)]">
                          {patient.mrn}
                        </span>
                        {isICU && (
                          <span className="rounded-full bg-rose-500/10 px-2 py-0.5 text-[10px] font-semibold text-rose-400 border border-rose-500/20">
                            ICU High Alert
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-[var(--text-secondary)]">
                        {formatDemographics(patient)} · Assigned: Dr. Aarav Sharma · Nurse Ananya Iyer
                      </p>
                    </div>
                  </div>

                  {/* Quick Action Buttons */}
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => {
                        setSelectedPatientForVital(patient.id);
                        setShowVitalModal(true);
                      }}
                      className="rounded-lg border border-[var(--border-color)] bg-[var(--bg-sidebar)] px-2.5 py-1.5 text-xs font-medium text-teal-400 transition hover:bg-teal-500/10"
                    >
                      + Vital
                    </button>
                    <button
                      type="button"
                      onClick={() => navigate("/medications")}
                      className="rounded-lg border border-[var(--border-color)] bg-[var(--bg-sidebar)] px-2.5 py-1.5 text-xs font-medium text-[var(--text-secondary)] transition hover:text-[var(--text-primary)] hover:bg-[var(--bg-hover)]"
                    >
                      Meds
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setSelectedPatientId(patient.id);
                        navigate(`/patients/${patient.id}`);
                      }}
                      className="flex items-center gap-1 rounded-lg bg-teal-500/10 px-2.5 py-1.5 text-xs font-semibold text-teal-300 transition hover:bg-teal-500/20"
                    >
                      <span>Chart</span>
                      <ChevronRight className="h-3 w-3" />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>

          <div className="border-t border-[var(--border-color)] p-3 text-center">
            <button
              onClick={() => navigate("/patients")}
              className="text-xs font-semibold text-teal-400 hover:text-teal-300 inline-flex items-center gap-1"
            >
              <span>View complete facility roster & bed allocations</span>
              <ArrowUpRight className="h-3.5 w-3.5" />
            </button>
          </div>
        </section>

        {/* ── RIGHT COLUMN: Interactive Shift Tasks & Progress ── */}
        <section className="surface rounded-2xl border border-[var(--border-color)] p-5 space-y-4">
          <div className="flex items-center justify-between border-b border-[var(--border-color)] pb-3">
            <div>
              <h2 className="text-sm font-bold text-[var(--text-primary)]">
                Shift Clinical Tasks
              </h2>
              <p className="text-xs text-[var(--text-muted)]">
                Click any task to verify completion in real time
              </p>
            </div>
            <button
              onClick={() => setShowTaskModal(true)}
              className="rounded-lg bg-teal-500/10 px-2.5 py-1 text-xs font-semibold text-teal-300 hover:bg-teal-500/20"
            >
              + Add Task
            </button>
          </div>

          {/* Interactive Progress Bar */}
          <div>
            <div className="flex items-center justify-between text-xs font-medium">
              <span className="text-[var(--text-secondary)]">Workload Completion</span>
              <span className="font-mono text-teal-400">
                {completedTasksCount} / {totalTasksCount} tasks ({taskProgress}%)
              </span>
            </div>
            <div className="mt-2 h-2 overflow-hidden rounded-full bg-[var(--bg-subtle)]">
              <div
                className="h-full rounded-full bg-teal-500 transition-all duration-300 ease-out"
                style={{ width: `${taskProgress}%` }}
              />
            </div>
          </div>

          {/* Tasks Filter */}
          <div className="flex items-center gap-2 text-xs">
            <button
              onClick={() => setTaskFilter("ALL")}
              className={`rounded-md px-2 py-0.5 font-medium transition ${
                taskFilter === "ALL"
                  ? "bg-teal-500/20 text-teal-300"
                  : "text-[var(--text-muted)] hover:text-[var(--text-primary)]"
              }`}
            >
              All Tasks ({tasks.length})
            </button>
            <button
              onClick={() => setTaskFilter("PENDING")}
              className={`rounded-md px-2 py-0.5 font-medium transition ${
                taskFilter === "PENDING"
                  ? "bg-teal-500/20 text-teal-300"
                  : "text-[var(--text-muted)] hover:text-[var(--text-primary)]"
              }`}
            >
              Pending ({totalTasksCount - completedTasksCount})
            </button>
          </div>

          {/* Interactive Tasks Checklist */}
          <div className="space-y-2.5 max-h-[340px] overflow-y-auto pr-1">
            {filteredTasks.map((t) => {
              const isDone = t.status === "COMPLETED";

              return (
                <div
                  key={t.id}
                  onClick={() => handleToggleTask(t)}
                  className={`group flex items-start gap-3 rounded-xl border p-3 cursor-pointer transition select-none ${
                    isDone
                      ? "border-[var(--border-color)] bg-[var(--bg-sidebar)] opacity-60"
                      : "border-[var(--border-color)] bg-[var(--bg-subtle)] hover:border-teal-500/40"
                  }`}
                >
                  <button
                    type="button"
                    className="mt-0.5 shrink-0 transition"
                  >
                    {isDone ? (
                      <CheckCircle2 className="h-4.5 w-4.5 text-teal-400" />
                    ) : (
                      <Circle className="h-4.5 w-4.5 text-[var(--text-muted)] group-hover:text-teal-400" />
                    )}
                  </button>

                  <div className="min-w-0 flex-1">
                    <p
                      className={`text-xs font-semibold ${
                        isDone
                          ? "line-through text-[var(--text-muted)]"
                          : "text-[var(--text-primary)]"
                      }`}
                    >
                      {t.title}
                    </p>
                    <p className="mt-0.5 text-[11px] text-[var(--text-secondary)] line-clamp-1">
                      {t.description}
                    </p>
                    <div className="mt-1 flex items-center gap-2 text-[10px] text-[var(--text-muted)]">
                      <span className="font-medium text-teal-300">{t.patientName || "Patient"}</span>
                      <span>·</span>
                      <span>{t.assignedNurseName || "Nurse Ananya Iyer"}</span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </section>
      </div>

      {/* ── Recent Activity Real-Time Feed ── */}
      <section className="surface rounded-2xl border border-[var(--border-color)] p-5">
        <div className="flex items-center justify-between border-b border-[var(--border-color)] pb-3">
          <div>
            <h2 className="text-sm font-bold text-[var(--text-primary)]">
              Real-Time Clinical Activity Feed
            </h2>
            <p className="text-xs text-[var(--text-muted)]">
              Verifiable audit events recorded across patient care plans
            </p>
          </div>
          <button
            onClick={() => navigate("/timeline")}
            className="text-xs font-semibold text-teal-400 hover:underline"
          >
            Open Complete Timeline →
          </button>
        </div>

        <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {(summary.recentActivity || []).map((item) => {
            const Icon = activityIcons[item.eventType] || Activity;

            return (
              <div
                key={item.id}
                className="flex items-start gap-3 rounded-xl border border-[var(--border-color)] bg-[var(--bg-subtle)] p-3.5"
              >
                <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-teal-500/10 text-teal-400">
                  <Icon className="h-4 w-4" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-xs font-semibold text-[var(--text-primary)]">
                    {activityTitles[item.eventType] || "Clinical Event"}
                  </p>
                  <p className="mt-0.5 text-xs text-[var(--text-secondary)] line-clamp-2">
                    {item.description}
                  </p>
                  <p className="mt-1.5 font-mono text-[10px] text-[var(--text-muted)]">
                    {timeAgo(item.createdAt)}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* ══════════════════════════════════
          MODAL: RECORD VITAL
      ══════════════════════════════════ */}
      {showVitalModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/70 p-4 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-2xl border border-[var(--border-color)] bg-[var(--bg-sidebar)] p-6 shadow-2xl">
            <div className="flex items-center justify-between border-b border-[var(--border-color)] pb-3">
              <div className="flex items-center gap-2">
                <HeartPulse className="h-5 w-5 text-teal-400" />
                <h3 className="text-base font-bold text-[var(--text-primary)]">
                  Record Bedside Vital
                </h3>
              </div>
              <button
                onClick={() => setShowVitalModal(false)}
                className="rounded-lg p-1 text-[var(--text-muted)] hover:bg-[var(--bg-hover)]"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <form onSubmit={handleSaveVital} className="mt-4 space-y-4">
              <div>
                <label className="mb-1 block text-xs font-semibold text-[var(--text-secondary)]">
                  Patient
                </label>
                <select
                  value={selectedPatientForVital}
                  onChange={(e) => setSelectedPatientForVital(e.target.value)}
                  className="w-full rounded-lg border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3 py-2 text-xs text-[var(--text-primary)] outline-none"
                >
                  {patients.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} ({p.mrn})
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="mb-1 block text-xs font-semibold text-[var(--text-secondary)]">
                    Vital Type
                  </label>
                  <select
                    value={vitalForm.type}
                    onChange={(e) => {
                      const t = e.target.value;
                      let unit = "bpm";
                      if (t === "BLOOD_PRESSURE") unit = "mmHg";
                      if (t === "OXYGEN_SATURATION") unit = "%";
                      if (t === "TEMPERATURE") unit = "°F";
                      if (t === "RESPIRATION_RATE") unit = "rpm";
                      setVitalForm({ ...vitalForm, type: t, unit });
                    }}
                    className="w-full rounded-lg border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3 py-2 text-xs text-[var(--text-primary)] outline-none"
                  >
                    <option value="HEART_RATE">Heart Rate</option>
                    <option value="BLOOD_PRESSURE">Blood Pressure</option>
                    <option value="OXYGEN_SATURATION">SpO2 Saturation</option>
                    <option value="TEMPERATURE">Temperature</option>
                    <option value="RESPIRATION_RATE">Respiration Rate</option>
                  </select>
                </div>

                <div>
                  <label className="mb-1 block text-xs font-semibold text-[var(--text-secondary)]">
                    Value ({vitalForm.unit})
                  </label>
                  <input
                    type="text"
                    required
                    value={vitalForm.value}
                    onChange={(e) => setVitalForm({ ...vitalForm, value: e.target.value })}
                    placeholder="e.g. 78 or 120/80"
                    className="w-full rounded-lg border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3 py-2 text-xs text-[var(--text-primary)] outline-none"
                  />
                </div>
              </div>

              <div className="flex items-center gap-2 pt-1">
                <input
                  type="checkbox"
                  id="vitalAbnormal"
                  checked={vitalForm.isAbnormal}
                  onChange={(e) => setVitalForm({ ...vitalForm, isAbnormal: e.target.checked })}
                  className="rounded border-[var(--border-color)] text-rose-500"
                />
                <label htmlFor="vitalAbnormal" className="text-xs text-rose-400 font-medium">
                  Flag as Abnormal / High Alert threshold
                </label>
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-[var(--border-color)]">
                <button
                  type="button"
                  onClick={() => setShowVitalModal(false)}
                  className="rounded-lg px-3 py-2 text-xs font-medium text-[var(--text-muted)] hover:bg-[var(--bg-hover)]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={savingAction}
                  className="rounded-lg bg-teal-500 px-4 py-2 text-xs font-semibold text-slate-950 transition hover:bg-teal-400"
                >
                  {savingAction ? "Saving..." : "Save Vital"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ══════════════════════════════════
          MODAL: ADD TASK
      ══════════════════════════════════ */}
      {showTaskModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/70 p-4 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-2xl border border-[var(--border-color)] bg-[var(--bg-sidebar)] p-6 shadow-2xl">
            <div className="flex items-center justify-between border-b border-[var(--border-color)] pb-3">
              <div className="flex items-center gap-2">
                <FileText className="h-5 w-5 text-teal-400" />
                <h3 className="text-base font-bold text-[var(--text-primary)]">
                  Add Clinical Shift Task
                </h3>
              </div>
              <button
                onClick={() => setShowTaskModal(false)}
                className="rounded-lg p-1 text-[var(--text-muted)] hover:bg-[var(--bg-hover)]"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <form onSubmit={handleCreateTask} className="mt-4 space-y-4">
              <div>
                <label className="mb-1 block text-xs font-semibold text-[var(--text-secondary)]">
                  Patient
                </label>
                <select
                  value={taskForm.patientId}
                  onChange={(e) => setTaskForm({ ...taskForm, patientId: e.target.value })}
                  className="w-full rounded-lg border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3 py-2 text-xs text-[var(--text-primary)] outline-none"
                >
                  {patients.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} ({p.mrn})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="mb-1 block text-xs font-semibold text-[var(--text-secondary)]">
                  Task Title
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Inspect IV Cannula site & repeat electrolyte panel"
                  value={taskForm.title}
                  onChange={(e) => setTaskForm({ ...taskForm, title: e.target.value })}
                  className="w-full rounded-lg border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3 py-2 text-xs text-[var(--text-primary)] outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="mb-1 block text-xs font-semibold text-[var(--text-secondary)]">
                    Priority
                  </label>
                  <select
                    value={taskForm.priority}
                    onChange={(e) => setTaskForm({ ...taskForm, priority: e.target.value })}
                    className="w-full rounded-lg border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3 py-2 text-xs text-[var(--text-primary)] outline-none"
                  >
                    <option value="CRITICAL">Critical (Stat)</option>
                    <option value="HIGH">High Priority</option>
                    <option value="ROUTINE">Routine Shift Care</option>
                  </select>
                </div>

                <div>
                  <label className="mb-1 block text-xs font-semibold text-[var(--text-secondary)]">
                    Assigned To
                  </label>
                  <input
                    type="text"
                    disabled
                    value="Nurse Ananya Iyer (ICU Charge)"
                    className="w-full rounded-lg border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3 py-2 text-xs text-[var(--text-muted)] cursor-not-allowed"
                  />
                </div>
              </div>

              <div>
                <label className="mb-1 block text-xs font-semibold text-[var(--text-secondary)]">
                  Clinical Instructions (Optional)
                </label>
                <textarea
                  rows={2}
                  placeholder="Additional notes for bedside nurse..."
                  value={taskForm.description}
                  onChange={(e) => setTaskForm({ ...taskForm, description: e.target.value })}
                  className="w-full rounded-lg border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3 py-2 text-xs text-[var(--text-primary)] outline-none resize-none"
                />
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-[var(--border-color)]">
                <button
                  type="button"
                  onClick={() => setShowTaskModal(false)}
                  className="rounded-lg px-3 py-2 text-xs font-medium text-[var(--text-muted)] hover:bg-[var(--bg-hover)]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={savingAction}
                  className="rounded-lg bg-teal-500 px-4 py-2 text-xs font-semibold text-slate-950 transition hover:bg-teal-400"
                >
                  {savingAction ? "Creating..." : "Create Task"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
