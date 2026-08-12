import { useState } from "react";
import { ArrowRightLeft, Users, Clock } from "lucide-react";
import { usePatients } from "../services/patientHooks";

export default function ShiftHandoffsPage() {
  const { patients, loading, error } = usePatients();
  const [selectedPatientId, setSelectedPatientId] = useState("");

  const selectedPatient = patients.find((p) => p.id === selectedPatientId);

  return (
    <div className="space-y-6 pb-10">
      <div>
        <p className="text-xs font-medium text-teal-400">Care transitions</p>
        <h1 className="mt-1 text-xl font-semibold">Shift handoffs</h1>
        <p className="mt-1 text-xs text-[var(--text-muted)]">
          Transfer patient care between shifts and teams
        </p>
      </div>

      <div className="surface rounded-2xl p-4">
        <div className="flex items-center gap-2">
          <Users className="h-4 w-4 text-[var(--text-muted)]" />
          <select
            value={selectedPatientId}
            onChange={(e) => setSelectedPatientId(e.target.value)}
            className="h-10 rounded-xl border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3 text-xs text-[var(--text-primary)] outline-none focus:border-teal-500/50"
          >
            <option value="">Select a patient for handoff</option>
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

      {!loading && !error && selectedPatient && (
        <div className="space-y-4">
          <div className="surface rounded-2xl p-5">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-teal-500/10 text-teal-400">
                <ArrowRightLeft className="h-5 w-5" />
              </div>
              <div>
                <p className="text-sm font-medium">{selectedPatient.name}</p>
                <p className="text-xs text-[var(--text-muted)]">
                  {selectedPatient.mrn}
                </p>
              </div>
            </div>

            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              <div className="rounded-xl bg-[var(--bg-subtle)] p-4">
                <p className="text-[11px] font-medium uppercase tracking-wider text-[var(--text-muted)]">
                  Current status
                </p>
                <p className="mt-1 text-sm">Active patient record</p>
              </div>
              <div className="rounded-xl bg-[var(--bg-subtle)] p-4">
                <p className="text-[11px] font-medium uppercase tracking-wider text-[var(--text-muted)]">
                  Last updated
                </p>
                <p className="mt-1 text-sm flex items-center gap-1">
                  <Clock className="h-3 w-3" />
                  {selectedPatient.createdAt
                    ? new Date(selectedPatient.createdAt).toLocaleDateString()
                    : "Unknown"}
                </p>
              </div>
            </div>

            <div className="mt-4">
              <p className="text-xs font-medium">Handoff notes</p>
              <textarea
                placeholder="Enter handoff notes for the next shift..."
                className="mt-2 h-24 w-full rounded-xl border border-[var(--border-color)] bg-[var(--bg-subtle)] p-3 text-xs text-[var(--text-primary)] outline-none focus:border-teal-500/50 resize-none"
              />
            </div>

            <div className="mt-4 flex justify-end">
              <button
                type="button"
                className="rounded-xl bg-teal-500 px-4 py-2 text-xs font-semibold text-slate-950 hover:bg-teal-400 transition"
              >
                Create handoff
              </button>
            </div>
          </div>
        </div>
      )}

      {!loading && !error && !selectedPatientId && (
        <div className="surface rounded-2xl p-8 text-center">
          <ArrowRightLeft className="mx-auto h-8 w-8 text-[var(--text-muted)]" />
          <p className="mt-3 text-sm font-medium">
            Select a patient to create a handoff
          </p>
          <p className="mt-1 text-xs text-[var(--text-muted)]">
            Choose from the dropdown above to start a care transition.
          </p>
        </div>
      )}
    </div>
  );
}
