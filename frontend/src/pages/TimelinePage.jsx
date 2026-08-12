import { useState } from "react";
import { FileClock, Filter } from "lucide-react";
import { usePatients } from "../services/patientHooks";
import TimelineFeed from "../components/clinical/TimelineFeed";

export default function TimelinePage() {
  const { patients, loading, error } = usePatients();
  const [selectedPatientId, setSelectedPatientId] = useState("");

  return (
    <div className="space-y-6 pb-10">
      <div>
        <p className="text-xs font-medium text-teal-400">Clinical records</p>
        <h1 className="mt-1 text-xl font-semibold">Timeline</h1>
        <p className="mt-1 text-xs text-[var(--text-muted)]">
          Chronological view of all patient care activity
        </p>
      </div>

      <div className="surface rounded-2xl p-4">
        <div className="flex items-center gap-2">
          <Filter className="h-4 w-4 text-[var(--text-muted)]" />
          <select
            value={selectedPatientId}
            onChange={(e) => setSelectedPatientId(e.target.value)}
            className="h-10 rounded-xl border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3 text-xs text-[var(--text-primary)] outline-none focus:border-teal-500/50"
          >
            <option value="">All patients</option>
            {patients.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} — {p.mrn}
              </option>
            ))}
          </select>
        </div>
      </div>

      {loading && (
        <div className="surface rounded-2xl p-8 text-center text-sm text-[var(--text-muted)]">
          Loading patients...
        </div>
      )}

      {error && (
        <div className="surface rounded-2xl p-8 text-center text-sm text-rose-400">
          Unable to load patients.
        </div>
      )}

      {!loading && !error && (
        <div className="space-y-4">
          {selectedPatientId ? (
            <TimelineFeed patientId={selectedPatientId} />
          ) : (
            <div className="surface rounded-2xl p-8 text-center">
              <FileClock className="mx-auto h-8 w-8 text-[var(--text-muted)]" />
              <p className="mt-3 text-sm font-medium">
                Select a patient to view their timeline
              </p>
              <p className="mt-1 text-xs text-[var(--text-muted)]">
                Choose from the dropdown above to see clinical events.
              </p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
