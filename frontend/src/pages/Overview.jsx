import {
  ArrowUpRight,
  CalendarDays,
  ChevronRight,
  Clock3,
  FileText,
  HeartPulse,
  Pill,
  ShieldAlert,
  Sparkles,
  Stethoscope,
  Users,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import { usePatients } from "../services/patientHooks";
import { useStore } from "../store/useStore";
import { useAuth } from "../context/AuthContext";



const activity = [
  {
    icon: HeartPulse,
    title: "Vital recorded",
    description: "James Wilson � Oxygen saturation 88%",
    time: "2 min ago",
  },
  {
    icon: FileText,
    title: "Task completed",
    description: "Medication reconciliation � Elena Martinez",
    time: "18 min ago",
  },
  {
    icon: Users,
    title: "Shift handoff created",
    description: "Robert Chen � Evening shift",
    time: "31 min ago",
  },
];

function StatusDot({ tone }) {
  const classes = {
    critical: "bg-rose-400",
    warning: "bg-amber-400",
    neutral: "bg-teal-400",
  };

  return <span className={`h-2 w-2 rounded-full ${classes[tone]}`} />;
}

export default function Overview() {
  const navigate = useNavigate();
  const { toggleCopilot, setSelectedPatientId } = useStore();
  const { user: currentUser } = useAuth();
  const { patients, loading: patientsLoading, error: patientsError } = usePatients();

  const firstName = currentUser?.fullName?.split(" ")[0] || "there";

  const openPatient = (patient) => {
    setSelectedPatientId(patient.id);
    navigate(`/patients/${patient.id}`);
  };

  return (
    <div className="space-y-7 pb-8">
      <section className="flex flex-col justify-between gap-5 pt-2 sm:flex-row sm:items-end">
        <div>
          <p className="mb-2 text-xs font-medium uppercase tracking-[0.16em] text-teal-500">
            Saturday � August 8, 2026
          </p>

          <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">
            Good morning, {firstName}.
          </h1>

          <p className="mt-2 max-w-xl text-sm leading-6 text-[var(--text-secondary)]">
            Here&apos;s what needs your attention across the care team today.
          </p>
        </div>

        <button
          type="button"
          onClick={toggleCopilot}
          className="group inline-flex w-fit items-center gap-2 rounded-xl border border-teal-500/20 bg-teal-500/5 px-4 py-2.5 text-sm font-medium text-teal-500 transition hover:border-teal-500/40 hover:bg-teal-500/10"
        >
          <Sparkles className="h-4 w-4 transition-transform group-hover:rotate-6" />
          Ask CareFlow Copilot
          <ArrowUpRight className="h-3.5 w-3.5 opacity-60" />
        </button>
      </section>

      <section className="grid gap-4 md:grid-cols-3">
        <div className="surface rounded-2xl p-5">
          <div className="flex items-start justify-between">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-rose-500/10 text-rose-400">
              <ShieldAlert className="h-5 w-5" />
            </div>
            <span className="text-xs text-[var(--text-muted)]">Now</span>
          </div>
          <p className="mt-5 text-2xl font-semibold">1</p>
          <p className="mt-1 text-sm text-[var(--text-secondary)]">
            Critical attention
          </p>
        </div>

        <div className="surface rounded-2xl p-5">
          <div className="flex items-start justify-between">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-500/10 text-amber-400">
              <Clock3 className="h-5 w-5" />
            </div>
            <span className="text-xs text-[var(--text-muted)]">Today</span>
          </div>
          <p className="mt-5 text-2xl font-semibold">4</p>
          <p className="mt-1 text-sm text-[var(--text-secondary)]">
            Tasks awaiting action
          </p>
        </div>

        <div className="surface rounded-2xl p-5">
          <div className="flex items-start justify-between">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-teal-500/10 text-teal-400">
              <CalendarDays className="h-5 w-5" />
            </div>
            <span className="text-xs text-[var(--text-muted)]">Today</span>
          </div>
          <p className="mt-5 text-2xl font-semibold">6</p>
          <p className="mt-1 text-sm text-[var(--text-secondary)]">
            Upcoming appointments
          </p>
        </div>
      </section>

      <div className="grid gap-5 xl:grid-cols-[minmax(0,1.55fr)_minmax(320px,0.85fr)]">
        <section className="surface overflow-hidden rounded-2xl">
          <div className="flex items-center justify-between border-b border-[var(--border-color)] px-5 py-4">
            <div>
              <h2 className="text-sm font-semibold">Needs your attention</h2>
              <p className="mt-1 text-xs text-[var(--text-muted)]">
                Prioritized from recent clinical activity
              </p>
            </div>
          </div>

          <div className="divide-y divide-[var(--border-color)]">
            {patientsLoading && (
          <div className="px-5 py-8 text-center text-sm text-[var(--text-muted)]">Loading patients...</div>
        )}

        {patientsError && !patientsLoading && (
          <div className="px-5 py-8 text-center text-sm text-rose-400">Unable to load patients right now.</div>
        )}

        {!patientsLoading && !patientsError && patients.length === 0 && (
          <div className="px-5 py-8 text-center text-sm text-[var(--text-muted)]">No patients found.</div>
        )}

        {!patientsLoading && !patientsError && patients.map((patient) => (
              <button
                key={patient.id}
                type="button"
                onClick={() => openPatient(patient)}
                className="group flex w-full items-center gap-4 px-5 py-4 text-left transition hover:bg-[var(--bg-hover)]"
              >
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[var(--bg-subtle)] text-sm font-semibold text-[var(--text-secondary)]">
                  {patient.name
                    .split(" ")
                    .map((part) => part[0])
                    .join("")}
                </div>

                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-sm font-medium">
                      {patient.name}
                    </span>
                    <span className="text-[10px] text-[var(--text-muted)]">
                      {patient.mrn}
                    </span>
                  </div>

                  <div className="mt-1 flex items-center gap-2">
                    <StatusDot tone="neutral" />
                    <span className="text-xs text-[var(--text-secondary)]">
                      Patient record available
                    </span>
                  </div>
                </div>

                <div className="hidden text-right sm:block">
                  <p className="text-xs font-medium">Active patient</p>
                  <p className="mt-1 text-[10px] text-[var(--text-muted)]">
                    Open record
                  </p>
                </div>

                <ChevronRight className="h-4 w-4 shrink-0 text-[var(--text-muted)] transition group-hover:translate-x-0.5 group-hover:text-teal-400" />
              </button>
            ))}
          </div>

          <button
            type="button"
            onClick={() => navigate("/patients")}
            className="flex w-full items-center justify-center gap-2 border-t border-[var(--border-color)] px-5 py-3 text-xs font-medium text-teal-500 transition hover:bg-[var(--bg-subtle)]"
          >
            View all patients
            <ArrowUpRight className="h-3.5 w-3.5" />
          </button>
        </section>

        <section className="surface rounded-2xl p-5">
          <div className="flex items-start justify-between">
            <div>
              <h2 className="text-sm font-semibold">Today&apos;s care</h2>
              <p className="mt-1 text-xs text-[var(--text-muted)]">
                Your clinical workload
              </p>
            </div>
            <Stethoscope className="h-5 w-5 text-teal-400" />
          </div>

          <div className="mt-6 space-y-5">
            <div>
              <div className="flex items-center justify-between text-xs">
                <span className="text-[var(--text-secondary)]">
                  Tasks completed
                </span>
                <span className="font-medium">8 / 12</span>
              </div>

              <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-[var(--bg-subtle)]">
                <div className="h-full w-[67%] rounded-full bg-teal-500" />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="rounded-xl bg-[var(--bg-subtle)] p-4">
                <p className="text-xl font-semibold">12</p>
                <p className="mt-1 text-[11px] text-[var(--text-muted)]">
                  Active patients
                </p>
              </div>

              <div className="rounded-xl bg-[var(--bg-subtle)] p-4">
                <p className="text-xl font-semibold">2</p>
                <p className="mt-1 text-[11px] text-[var(--text-muted)]">
                  Handoffs
                </p>
              </div>
            </div>

            <div className="rounded-xl border border-teal-500/15 bg-teal-500/5 p-4">
              <div className="flex gap-3">
                <div className="mt-0.5 h-7 w-7 shrink-0 rounded-lg bg-teal-500/10 p-1.5 text-teal-400">
                  <Sparkles className="h-full w-full" />
                </div>

                <div>
                  <p className="text-xs font-medium">A quieter way to review</p>
                  <p className="mt-1 text-[11px] leading-5 text-[var(--text-secondary)]">
                    Copilot can summarize recent patient context before your
                    next review.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>

      <section className="surface rounded-2xl">
        <div className="flex items-center justify-between border-b border-[var(--border-color)] px-5 py-4">
          <div>
            <h2 className="text-sm font-semibold">Recent activity</h2>
            <p className="mt-1 text-xs text-[var(--text-muted)]">
              Latest events across your workspace
            </p>
          </div>

          <button
            type="button"
            onClick={() => navigate("/timeline")}
            className="text-xs font-medium text-teal-500 hover:text-teal-400"
          >
            Open timeline
          </button>
        </div>

        <div className="grid md:grid-cols-3">
          {activity.map(({ icon: Icon, title, description, time }, index) => (
            <div
              key={title}
              className={[
                "flex gap-3 px-5 py-4",
                index !== 0 ? "border-t md:border-l md:border-t-0" : "",
                "border-[var(--border-color)]",
              ].join(" ")}
            >
              <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-[var(--bg-subtle)] text-teal-400">
                <Icon className="h-4 w-4" />
              </div>

              <div className="min-w-0">
                <p className="text-xs font-medium">{title}</p>
                <p className="mt-1 text-[11px] leading-5 text-[var(--text-secondary)]">
                  {description}
                </p>
                <p className="mt-1.5 text-[10px] text-[var(--text-muted)]">
                  {time}
                </p>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}




