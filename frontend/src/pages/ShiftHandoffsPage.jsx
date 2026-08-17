import { useState, useEffect } from "react";
import { ArrowRightLeft, Users, Clock, FileText } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import api from "../services/api";
import { getMyPatients } from "../services/patientApi";

export default function ShiftHandoffsPage() {
  const { user } = useAuth();
  const [patients, setPatients] = useState([]);
  const [selectedPatientId, setSelectedPatientId] = useState("");
  const [handoffs, setHandoffs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [notes, setNotes] = useState("");

  useEffect(() => {
    getMyPatients()
      .then((data) => {
        setPatients(data || []);
        if (data?.length === 1) setSelectedPatientId(data[0].id);
      })
      .catch(() => setPatients([]))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!selectedPatientId) return;
    api
      .get(`/shift-handoffs/patient/${selectedPatientId}`)
      .then((res) => setHandoffs(res.data || []))
      .catch(() => setHandoffs([]));
  }, [selectedPatientId]);

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

      {patients.length > 1 && (
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
      )}

      {loading && (
        <div className="surface rounded-2xl p-8 text-center text-sm text-[var(--text-muted)]">
          Loading...
        </div>
      )}

      {!loading && selectedPatient && (
        <div className="space-y-4">
          <div className="surface rounded-2xl p-5">
            <div className="flex items-center gap-3 mb-4">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-teal-500/10 text-teal-400">
                <ArrowRightLeft className="h-5 w-5" />
              </div>
              <div>
                <p className="text-sm font-medium">{selectedPatient.name}</p>
                <p className="text-xs text-[var(--text-muted)]">{selectedPatient.mrn}</p>
              </div>
            </div>

            <div className="space-y-3">
              <div>
                <label className="text-xs font-medium text-[var(--text-muted)]">Handoff notes</label>
                <textarea
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Enter handoff notes for the next shift..."
                  className="mt-1 h-24 w-full rounded-xl border border-[var(--border-color)] bg-[var(--bg-subtle)] p-3 text-xs text-[var(--text-primary)] outline-none focus:border-teal-500/50 resize-none"
                />
              </div>

              <div className="flex justify-end">
                <button
                  type="button"
                  className="rounded-xl bg-teal-500 px-4 py-2 text-xs font-semibold text-slate-950 hover:bg-teal-400 transition"
                >
                  Create handoff
                </button>
              </div>
            </div>
          </div>

          {handoffs.length > 0 && (
            <div className="space-y-3">
              <h3 className="text-sm font-semibold">Previous handoffs</h3>
              {handoffs.map((h) => (
                <div key={h.id} className="surface rounded-2xl p-4">
                  <div className="flex items-center gap-2 text-[var(--text-muted)] mb-2">
                    <Clock className="h-3 w-3" />
                    <span className="text-[10px]">{new Date(h.shiftDate).toLocaleDateString()}</span>
                  </div>
                  <p className="text-xs">{h.notes}</p>
                  {h.aiSummary && (
                    <div className="mt-2 rounded-lg bg-teal-500/5 px-3 py-2">
                      <p className="text-[10px] text-teal-400 font-medium">AI Summary</p>
                      <p className="text-[11px] text-[var(--text-muted)] mt-0.5">{h.aiSummary}</p>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {!loading && !selectedPatientId && patients.length > 0 && (
        <div className="surface rounded-2xl p-8 text-center">
          <ArrowRightLeft className="mx-auto h-8 w-8 text-[var(--text-muted)]" />
          <p className="mt-3 text-sm font-medium">Select a patient to create a handoff</p>
        </div>
      )}
    </div>
  );
}