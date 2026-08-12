import { useEffect, useState } from "react";
import { Activity, Clock3, Sparkles } from "lucide-react";
import { loadTimelineReplay } from "../../services/timelineHooks";
import { getPatientSummary } from "../../services/copilotApi";
import { useAuth } from "../../context/AuthContext";

function formatDate(value) {
  if (!value) return "Unknown time";
  return new Date(value).toLocaleString([], { dateStyle: "medium", timeStyle: "short" });
}

export default function TimelineReplay({ patientId }) {
  const { user } = useAuth();
  const [range, setRange] = useState("24h");
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [summary, setSummary] = useState(null);
  const [summaryLoading, setSummaryLoading] = useState(false);
  const [summaryError, setSummaryError] = useState(null);

  useEffect(() => {
    if (!patientId) return;
    let active = true;
    setLoading(true);
    setError(null);
    setSummary(null);
    setSummaryError(null);

    loadTimelineReplay(patientId, range)
      .then((data) => active && setEvents(data || []))
      .catch((err) => active && setError(err))
      .finally(() => active && setLoading(false));

    return () => {
      active = false;
    };
  }, [patientId, range]);

  function generateSummary() {
    if (!patientId || !user?.id) return;
    setSummaryLoading(true);
    setSummaryError(null);
    getPatientSummary(patientId, user.id)
      .then((text) => setSummary(text))
      .catch(() => setSummaryError("Copilot could not generate a summary right now."))
      .finally(() => setSummaryLoading(false));
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="inline-flex rounded-xl border border-[var(--border-color)] bg-[var(--bg-subtle)] p-1">
          {["24h", "7d"].map((r) => (
            <button
              key={r}
              type="button"
              onClick={() => setRange(r)}
              className={`rounded-lg px-3 py-1.5 text-xs font-medium transition ${
                range === r
                  ? "bg-teal-500 text-slate-950"
                  : "text-[var(--text-secondary)] hover:text-[var(--text-primary)]"
              }`}
            >
              {r === "24h" ? "Last 24 hours" : "Last 7 days"}
            </button>
          ))}
        </div>

        <button
          type="button"
          onClick={generateSummary}
          disabled={summaryLoading}
          className="inline-flex items-center gap-1.5 rounded-xl border border-teal-500/30 bg-teal-500/10 px-3 py-1.5 text-xs font-medium text-teal-400 transition hover:bg-teal-500/20 disabled:opacity-60"
        >
          <Sparkles className="h-3.5 w-3.5" />
          {summaryLoading ? "Summarizing..." : "AI summary"}
        </button>
      </div>

      {(summary || summaryError) && (
        <div
          className={`rounded-xl border px-4 py-3 text-xs leading-6 ${
            summaryError
              ? "border-rose-500/30 bg-rose-500/10 text-rose-300"
              : "border-teal-500/20 bg-teal-500/5 text-[var(--text-secondary)]"
          }`}
        >
          {summaryError || summary}
        </div>
      )}

      <div className="surface overflow-hidden rounded-2xl">
        {loading && (
          <div className="px-5 py-10 text-center text-sm text-[var(--text-muted)]">
            Replaying timeline...
          </div>
        )}

        {error && !loading && (
          <div className="px-5 py-10 text-center text-sm text-rose-400">
            Unable to load the replay window.
          </div>
        )}

        {!loading && !error && events.length === 0 && (
          <div className="px-5 py-10 text-center">
            <Clock3 className="mx-auto h-6 w-6 text-[var(--text-muted)]" />
            <p className="mt-3 text-sm font-medium">Nothing recorded in this window</p>
          </div>
        )}

        {!loading && !error && events.length > 0 && (
          <div className="divide-y divide-[var(--border-color)]">
            {events.map((event) => (
              <div key={event.id} className="flex gap-4 px-5 py-4">
                <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-teal-500/10 text-teal-400">
                  <Activity className="h-4 w-4" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-xs font-semibold">{event.eventType}</p>
                  <p className="mt-1 text-sm text-[var(--text-secondary)]">{event.description}</p>
                  <p className="mt-1 text-[10px] text-[var(--text-muted)]">{formatDate(event.createdAt)}</p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
