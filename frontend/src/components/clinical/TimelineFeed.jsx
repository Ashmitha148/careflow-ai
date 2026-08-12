import { useMemo, useRef, useState } from "react";
import {
  Activity,
  CalendarDays,
  FileText,
  HeartPulse,
  Pill,
  UserRound,
} from "lucide-react";
import { usePatientTimeline } from "../../services/timelineHooks";

const EVENT_TYPES = [
  "DIAGNOSIS",
  "MEDICATION",
  "MEDICATION_ADMINISTRATION",
  "VITAL",
  "SHIFT_HANDOFF",
  "CRITICAL_ALERT",
  "APPOINTMENT",
  "FILE_UPLOAD",
  "TASK",
];

const eventIcons = {
  DIAGNOSIS: Activity,
  MEDICATION: Pill,
  MEDICATION_ADMINISTRATION: Pill,
  VITAL: HeartPulse,
  SHIFT_HANDOFF: UserRound,
  CRITICAL_ALERT: Activity,
  APPOINTMENT: CalendarDays,
  FILE_UPLOAD: FileText,
  TASK: FileText,
};

function formatEventType(type) {
  return type
    ?.toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function formatDate(value) {
  if (!value) return "Unknown time";
  return new Date(value).toLocaleString([], { dateStyle: "medium", timeStyle: "short" });
}

export default function TimelineFeed({ patientId }) {
  const [filterType, setFilterType] = useState(null);
  const params = useMemo(
    () => (filterType ? { eventType: filterType } : {}),
    [filterType]
  );
  const { events, loading, error } = usePatientTimeline(patientId, params);
  const rowRefs = useRef({});

  function scrollToEvent(id) {
    const el = rowRefs.current[id];
    if (el) {
      el.scrollIntoView({ behavior: "smooth", block: "center" });
      el.classList.add("ring-1", "ring-teal-500/50");
      setTimeout(() => el.classList.remove("ring-1", "ring-teal-500/50"), 1500);
    }
  }

  return (
    <div className="surface overflow-hidden rounded-2xl">
      <div className="flex flex-wrap items-center gap-2 border-b border-[var(--border-color)] px-5 py-4">
        <button
          type="button"
          onClick={() => setFilterType(null)}
          className={`rounded-full px-3 py-1 text-[11px] font-medium transition ${
            !filterType
              ? "bg-teal-500 text-slate-950"
              : "bg-[var(--bg-subtle)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]"
          }`}
        >
          All
        </button>
        {EVENT_TYPES.map((type) => (
          <button
            key={type}
            type="button"
            onClick={() => setFilterType(type)}
            className={`rounded-full px-3 py-1 text-[11px] font-medium transition ${
              filterType === type
                ? "bg-teal-500 text-slate-950"
                : "bg-[var(--bg-subtle)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]"
            }`}
          >
            {formatEventType(type)}
          </button>
        ))}
      </div>

      {loading && (
        <div className="px-5 py-10 text-center text-sm text-[var(--text-muted)]">
          Loading clinical timeline...
        </div>
      )}

      {error && !loading && (
        <div className="px-5 py-10 text-center text-sm text-rose-400">
          Unable to load the clinical timeline.
        </div>
      )}

      {!loading && !error && events.length === 0 && (
        <div className="px-5 py-10 text-center">
          <Activity className="mx-auto h-6 w-6 text-[var(--text-muted)]" />
          <p className="mt-3 text-sm font-medium">No timeline events yet</p>
          <p className="mt-1 text-xs text-[var(--text-muted)]">
            Clinical activity will appear here as it is recorded.
          </p>
        </div>
      )}

      {!loading && !error && events.length > 0 && (
        <div className="divide-y divide-[var(--border-color)]">
          {events.map((event) => {
            const Icon = eventIcons[event.eventType] || Activity;
            return (
              <div
                key={event.id}
                ref={(el) => (rowRefs.current[event.id] = el)}
                className="group flex gap-4 rounded-lg px-5 py-5 transition hover:bg-[var(--bg-subtle)]"
              >
                <div className="relative flex w-9 shrink-0 justify-center">
                  <div className="relative z-10 flex h-9 w-9 items-center justify-center rounded-xl bg-teal-500/10 text-teal-400">
                    <Icon className="h-4 w-4" />
                  </div>
                </div>

                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-xs font-semibold">
                      {formatEventType(event.eventType)}
                    </span>

                    {event.correctsEventId && (
                      <button
                        type="button"
                        onClick={() => scrollToEvent(event.correctsEventId)}
                        className="rounded-full bg-amber-500/10 px-2 py-0.5 text-[10px] font-medium text-amber-400 hover:bg-amber-500/20"
                      >
                        Correction &middot; view original
                      </button>
                    )}
                  </div>

                  <p className="mt-1 text-sm leading-6 text-[var(--text-secondary)]">
                    {event.description}
                  </p>

                  <div className="mt-2 flex flex-wrap gap-x-3 gap-y-1 text-[10px] text-[var(--text-muted)]">
                    <span>{formatDate(event.createdAt)}</span>
                    {event.createdByName && <span>Recorded by {event.createdByName}</span>}
                    </div>

                    {event.eventType === "CRITICAL_ALERT" && event.criticalAlert && (
                      <div className="mt-3 rounded-xl border border-rose-500/20 bg-rose-500/5 px-4 py-3">
                        <p className="text-[11px] font-semibold text-rose-400">
                          Why was this alert triggered?
                        </p>

                        <div className="mt-2 grid grid-cols-2 gap-x-4 gap-y-1.5 text-[11px] leading-5 text-[var(--text-secondary)]">
                          <span>
                            Recorded value:{" "}
                            <strong className="text-rose-300">
                              {event.criticalAlert.recordedValue || "N/A"}
                            </strong>
                          </span>

                          <span>
                            Threshold:{" "}
                            <strong>{event.criticalAlert.threshold || "N/A"}</strong>
                          </span>

                          <span>
                            Timestamp: {formatDate(event.criticalAlert.timestamp)}
                          </span>

                          <span>
                            Recorded by: {event.criticalAlert.recordedByName || "N/A"}
                          </span>

                          <span className="col-span-2">
                            Notified:{" "}
                            {(event.criticalAlert.notifiedUsers || [])
                              .map((u) => u.fullName)
                              .join(", ") || "N/A"}
                          </span>
                        </div>
                      </div>
                    )}
                  </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
