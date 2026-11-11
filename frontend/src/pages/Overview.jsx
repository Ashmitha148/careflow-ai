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
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import { usePatients } from "../services/patientHooks";
import { useStore } from "../store/useStore";
import { useAuth } from "../context/AuthContext";
import { useEffect, useState } from "react";
import { getDashboardSummary } from "../services/dashboardApi";

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
  VITAL: "Vital recorded",
  TASK: "Task updated",
  SHIFT_HANDOFF: "Shift handoff recorded",
  MEDICATION: "Medication prescribed",
  MEDICATION_ADMINISTRATION: "Medication administered",
  DIAGNOSIS: "Clinical diagnosis",
  CRITICAL_ALERT: "Critical alert",
  APPOINTMENT: "Appointment scheduled",
  FILE_UPLOAD: "Clinical file uploaded",
};

function timeAgo(value) {
  if (!value) return "Unknown time";
  const seconds = Math.round((Date.now() - new Date(value).getTime()) / 1000);
  if (seconds < 60) return "just now";
  const minutes = Math.round(seconds / 60);
  if (minutes < 60) return minutes + "m ago";
  const hours = Math.round(minutes / 60);
  if (hours < 24) return hours + "h ago";
  const days = Math.round(hours / 24);
  return days + (days === 1 ? "d ago" : "d ago");
}

function StatusDot({ tone }) {
  const classes = {
    critical: "bg-rose-400",
    warning: "bg-amber-400",
    neutral: "bg-teal-400",
  };

  return <span className={`h-1.5 w-1.5 rounded-full ${classes[tone] || classes.neutral}`} />;
}

export default function Overview() {
  const navigate = useNavigate();
  const { toggleCopilot, setSelectedPatientId } = useStore();
  const { user: currentUser } = useAuth();
  const { patients, loading: patientsLoading, error: patientsError } = usePatients();

  const [summary, setSummary] = useState(null);
  const [summaryLoading, setSummaryLoading] = useState(true);
  const [summaryError, setSummaryError] = useState(null);

  useEffect(() => {
    let active = true;
    getDashboardSummary()
      .then((data) => { if (active) setSummary(data); })
      .catch((err) => { if (active) setSummaryError(err); })
      .finally(() => { if (active) setSummaryLoading(false); });
    return () => { active = false; };
  }, []);

  const stat = (value) => (summaryError ? "—" : summary ? value : "…");
  const tasksCompletedLabel = summaryError ? "—" : summary ? (summary.tasksCompleted + " / " + summary.tasksTotal) : "…";
  const taskProgress = summary && summary.tasksTotal > 0 ? Math.round((summary.tasksCompleted / summary.tasksTotal) * 100) : 0;
  const todayLabel = new Date().toLocaleDateString([], { weekday: "short", month: "short", day: "numeric", year: "numeric" });

  const openPatient = (patient) => {
    setSelectedPatientId(patient.id);
    navigate(`/patients/${patient.id}`);
  };

  return (
    <div className="space-y-6 pb-8">
      {/* ── Page Header: Context-focused headline, calm space, no greeting hype ── */}
      <section className="flex flex-col justify-between gap-4 pt-2 sm:flex-row sm:items-end">
        <div>
          <p className="mb-1 text-xs font-medium text-[var(--text-muted)]">
            {todayLabel} · Clinical operations
          </p>

          <h1 className="text-2xl font-semibold tracking-tight text-[var(--text-primary)] sm:text-3xl">
            Workspace Overview
          </h1>

          <p className="mt-1 text-sm text-[var(--text-secondary)]">
            Active patient continuity, team tasks, and telemetry events across your facility
          </p>
        </div>

        <button
          type="button"
          onClick={toggleCopilot}
          className="group inline-flex w-fit items-center gap-2 rounded-xl border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3.5 py-2 text-xs font-medium text-[var(--text-primary)] transition-colors hover:bg-[var(--bg-hover)]"
        >
          <Bot className="h-4 w-4 text-teal-400" />
          <span>Clinical assistant</span>
          <ArrowUpRight className="h-3.5 w-3.5 text-[var(--text-muted)] opacity-70" />
        </button>
      </section>

      {/* ── Metric Cards: Clear tabular figures, clean labels, subtle semantic indicator ── */}
      <section className="grid gap-4 md:grid-cols-3">
        <div className="surface rounded-xl p-5">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-[var(--text-muted)]">Critical alerts</span>
            <span className="h-2 w-2 rounded-full bg-rose-500" />
          </div>
          <p className="mt-3 text-3xl font-semibold tabular-nums text-[var(--text-primary)]">1</p>
          <p className="mt-1 text-xs text-[var(--text-secondary)]">Immediate physician review required</p>
        </div>

        <div className="surface rounded-xl p-5">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-[var(--text-muted)]">Pending actions</span>
            <span className="h-2 w-2 rounded-full bg-amber-500" />
          </div>
          <p className="mt-3 text-3xl font-semibold tabular-nums text-[var(--text-primary)]">{stat(summary?.tasksAwaitingAction)}</p>
          <p className="mt-1 text-xs text-[var(--text-secondary)]">Clinical tasks due on current shift</p>
        </div>

        <div className="surface rounded-xl p-5">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-[var(--text-muted)]">Scheduled appointments</span>
            <span className="h-2 w-2 rounded-full bg-teal-500" />
          </div>
          <p className="mt-3 text-3xl font-semibold tabular-nums text-[var(--text-primary)]">{stat(summary?.upcomingAppointments)}</p>
          <p className="mt-1 text-xs text-[var(--text-secondary)]">Next 24-hour outpatient window</p>
        </div>
      </section>

      <div className="grid gap-5 xl:grid-cols-[minmax(0,1.55fr)_minmax(320px,0.85fr)]">
        {/* ── Priority Patient Queue ── */}
        <section className="surface overflow-hidden rounded-xl">
          <div className="flex items-center justify-between border-b border-[var(--border-color)] px-5 py-3.5">
            <div>
              <h2 className="text-sm font-semibold text-[var(--text-primary)]">Needs attention</h2>
              <p className="mt-0.5 text-xs text-[var(--text-muted)]">
                Prioritized from recent timeline events and vitals
              </p>
            </div>
          </div>

          <div className="divide-y divide-[var(--border-color)]">
            {patientsLoading && (
              <div className="px-5 py-8 text-center text-xs text-[var(--text-muted)]">Loading patient records…</div>
            )}

            {patientsError && !patientsLoading && (
              <div className="px-5 py-8 text-center text-xs text-rose-400">Unable to load patient records.</div>
            )}

            {!patientsLoading && !patientsError && patients.length === 0 && (
              <div className="px-5 py-8 text-center text-xs text-[var(--text-muted)]">No active patients.</div>
            )}

            {!patientsLoading && !patientsError && patients.map((patient) => (
              <button
                key={patient.id}
                type="button"
                onClick={() => openPatient(patient)}
                className="group flex w-full items-center gap-4 px-5 py-3.5 text-left transition-colors hover:bg-[var(--bg-hover)]"
              >
                <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-[var(--bg-subtle)] text-xs font-semibold text-[var(--text-secondary)]">
                  {patient.name
                    .split(" ")
                    .map((part) => part[0])
                    .join("")}
                </div>

                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-xs font-medium text-[var(--text-primary)]">
                      {patient.name}
                    </span>
                    <span className="font-mono text-[10px] text-[var(--text-muted)]">
                      {patient.mrn}
                    </span>
                  </div>

                  <div className="mt-0.5 flex items-center gap-2">
                    <StatusDot tone="neutral" />
                    <span className="text-[11px] text-[var(--text-secondary)]">
                      Timeline and vitals active
                    </span>
                  </div>
                </div>

                <div className="hidden text-right sm:block">
                  <p className="text-xs font-medium text-[var(--text-primary)]">Active</p>
                  <p className="font-mono text-[10px] text-[var(--text-muted)]">
                    MRN {patient.mrn}
                  </p>
                </div>

                <ChevronRight className="h-4 w-4 shrink-0 text-[var(--text-muted)] transition-transform group-hover:translate-x-0.5" />
              </button>
            ))}
          </div>

          <button
            type="button"
            onClick={() => navigate("/patients")}
            className="flex w-full items-center justify-center gap-1.5 border-t border-[var(--border-color)] px-5 py-2.5 text-xs font-medium text-teal-400 transition-colors hover:bg-[var(--bg-subtle)]"
          >
            <span>View all patients</span>
            <ArrowUpRight className="h-3.5 w-3.5" />
          </button>
        </section>

        {/* ── Shift Workload & Context ── */}
        <section className="surface rounded-xl p-5">
          <div className="flex items-start justify-between">
            <div>
              <h2 className="text-sm font-semibold text-[var(--text-primary)]">Shift continuity</h2>
              <p className="mt-0.5 text-xs text-[var(--text-muted)]">
                Assigned workload
              </p>
            </div>
            <Stethoscope className="h-4 w-4 text-[var(--text-muted)]" />
          </div>

          <div className="mt-5 space-y-4">
            <div>
              <div className="flex items-center justify-between text-xs">
                <span className="text-[var(--text-secondary)]">
                  Completed tasks
                </span>
                <span className="font-mono font-medium text-[var(--text-primary)]">{tasksCompletedLabel}</span>
              </div>

              <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-[var(--bg-subtle)]">
                <div className="h-full rounded-full bg-teal-500" style={{ width: `${taskProgress}%` }} />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="rounded-lg bg-[var(--bg-subtle)] p-3">
                <p className="text-xl font-semibold tabular-nums text-[var(--text-primary)]">{stat(summary?.activePatients)}</p>
                <p className="mt-0.5 text-[11px] text-[var(--text-muted)]">
                  Active patients
                </p>
              </div>

              <div className="rounded-lg bg-[var(--bg-subtle)] p-3">
                <p className="text-xl font-semibold tabular-nums text-[var(--text-primary)]">{stat(summary?.handoffs)}</p>
                <p className="mt-0.5 text-[11px] text-[var(--text-muted)]">
                  Handoff reports
                </p>
              </div>
            </div>

            <div className="rounded-lg border border-[var(--border-color)] bg-[var(--bg-subtle)] p-3.5">
              <div className="flex items-start gap-2.5">
                <Bot className="mt-0.5 h-4 w-4 shrink-0 text-teal-400" />
                <div>
                  <p className="text-xs font-medium text-[var(--text-primary)]">Timeline synthesis</p>
                  <p className="mt-0.5 text-[11px] leading-relaxed text-[var(--text-secondary)]">
                    Generate an audit-grounded shift report with verifiable event citations before handoff.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>

      {/* ── Recent Activity Feed ── */}
      <section className="surface rounded-xl">
        <div className="flex items-center justify-between border-b border-[var(--border-color)] px-5 py-3.5">
          <div>
            <h2 className="text-sm font-semibold text-[var(--text-primary)]">Recent activity</h2>
            <p className="mt-0.5 text-xs text-[var(--text-muted)]">
              Real-time audit log of care team updates
            </p>
          </div>

          <button
            type="button"
            onClick={() => navigate("/timeline")}
            className="text-xs font-medium text-teal-400 hover:underline"
          >
            Open timeline
          </button>
        </div>

        {summaryLoading && (
          <div className="px-5 py-8 text-center text-xs text-[var(--text-muted)]">Loading recent events…</div>
        )}

        {summaryError && !summaryLoading && (
          <div className="px-5 py-8 text-center text-xs text-rose-400">Unable to load recent events.</div>
        )}

        {!summaryLoading && !summaryError && !(summary?.recentActivity?.length) && (
          <div className="px-5 py-8 text-center text-xs text-[var(--text-muted)]">No recent clinical events.</div>
        )}

        {!summaryLoading && !summaryError && (summary?.recentActivity || []).length > 0 && (
          <div className="grid md:grid-cols-3">
            {(summary?.recentActivity || []).map((item, index) => {
              const Icon = activityIcons[item.eventType] || FileText;
              return (
                <div
                  key={item.id}
                  className={[
                    "flex gap-3 px-5 py-3.5",
                    index !== 0 ? "border-t md:border-l md:border-t-0" : "",
                    "border-[var(--border-color)]",
                  ].join(" ")}
                >
                  <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-md bg-[var(--bg-subtle)] text-[var(--text-secondary)]">
                    <Icon className="h-3.5 w-3.5" />
                  </div>

                  <div className="min-w-0 flex-1">
                    <p className="text-xs font-medium text-[var(--text-primary)]">{activityTitles[item.eventType] || "Event"}</p>
                    <p className="mt-0.5 text-[11px] leading-relaxed text-[var(--text-secondary)]">
                      {item.description}
                    </p>
                    <p className="mt-1 font-mono text-[10px] text-[var(--text-muted)]">
                      {timeAgo(item.createdAt)}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}
