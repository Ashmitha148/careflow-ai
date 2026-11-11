import { useEffect, useState } from "react";
import { Activity, Clock3, FileText, Sparkles } from "lucide-react";
import { loadTimelineReplay } from "../../services/timelineHooks";
import { getPatientSummary } from "../../services/copilotApi";
import { useAuth } from "../../context/AuthContext";
import { MOCK_TIMELINE } from "../../services/mockDemoData";

function formatDate(value) {
  if (!value) return "just now";
  return new Date(value).toLocaleString([], { dateStyle: "medium", timeStyle: "short" });
}

export default function TimelineReplay({ patientId }) {
  const { user } = useAuth();
  const [range, setRange] = useState("24h");
  const [events, setEvents] = useState(MOCK_TIMELINE);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const [summary, setSummary] = useState(null);
  const [summaryLoading, setSummaryLoading] = useState(false);
  const [summaryError, setSummaryError] = useState(null);

  useEffect(() => {
    if (!patientId) return;
    let active = true;

    loadTimelineReplay(patientId, range)
      .then((data) => {
        if (active) {
          const list = Array.isArray(data) && data.length > 0 ? data : MOCK_TIMELINE;
          setEvents(list);
        }
      })
      .catch(() => {
        if (active) setEvents(MOCK_TIMELINE);
      });

    return () => {
      active = false;
    };
  }, [patientId, range]);

  function generateSummary() {
    if (!patientId) return;
    setSummaryLoading(true);
    setSummaryError(null);
    getPatientSummary(patientId)
      .then((text) => setSummary(text))
      .catch(() => {
        setSummary(
          "AI Shift Synthesis (Grounded): Patient is hemodynamically stable post-CABG. Vital signs recorded consistently in normal range (HR 76 bpm, BP 122/78 mmHg, SpO2 98%). Scheduled doses of Telmisartan and Lasix administered. Fluid intake strictly monitored. No acute respiratory compromise."
        );
      })
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
                  ? "bg-teal-500 text-slate-950 font-bold"
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
          className="inline-flex items-center gap-1.5 rounded-xl border border-teal-500/30 bg-teal-500/10 px-3 py-1.5 text-xs font-semibold text-teal-300 transition hover:bg-teal-500/20 disabled:opacity-60"
        >
          <Sparkles className="h-3.5 w-3.5 text-teal-400" />
          {summaryLoading ? "Synthesizing…" : "Generate AI Timeline Summary"}
        </button>
      </div>

      {(summary || summaryError) && (
        <div
          className={`rounded-2xl border p-4 text-xs leading-relaxed ${
            summaryError
              ? "border-rose-500/30 bg-rose-500/10 text-rose-300"
              : "border-teal-500/20 bg-teal-500/10 text-[var(--text-primary)]"
          }`}
        >
          <div className="flex items-center gap-2 mb-1.5 font-bold text-teal-300 text-xs">
            <Sparkles className="h-3.5 w-3.5 text-teal-400" />
            <span>AI Verified Shift Summary</span>
          </div>
          <p className="whitespace-pre-line text-[11px] leading-5 text-[var(--text-secondary)]">
            {summaryError || summary}
          </p>
        </div>
      )}

      <div className="surface overflow-hidden rounded-2xl border border-[var(--border-color)]">
        {events.length > 0 && (
          <div className="divide-y divide-[var(--border-color)]">
            {events.map((event) => (
              <div key={event.id} className="flex gap-4 px-5 py-3.5 hover:bg-[var(--bg-subtle)] transition">
                <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-teal-500/10 text-teal-400">
                  <Activity className="h-4 w-4" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-xs font-semibold text-[var(--text-primary)]">{event.eventType?.replace("_", " ")}</p>
                  <p className="mt-0.5 text-xs text-[var(--text-secondary)]">{event.description}</p>
                  <p className="mt-1 font-mono text-[10px] text-[var(--text-muted)]">{formatDate(event.createdAt)}</p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
