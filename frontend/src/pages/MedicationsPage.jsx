import { useState, useEffect } from "react";
import { Pill, Search, AlertTriangle } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { getMyPatients } from "../services/patientApi";
import { getPatientMedications } from "../services/clinicalApi";

function MedicationCard({ medication }) {
  return (
    <div className="surface rounded-2xl p-4">
      <div className="flex items-start justify-between">
        <div>
          <div className="flex items-center gap-2">
            <Pill className="h-4 w-4 text-teal-400" />
            <span className="text-sm font-medium">{medication.name}</span>
          </div>
          <p className="mt-1 text-xs text-[var(--text-muted)]">
            {medication.dosage} · {medication.frequency}
          </p>
        </div>
        <span
          className={`rounded-full px-2 py-0.5 text-[10px] font-medium ${
            medication.status === "ACTIVE"
              ? "bg-emerald-500/10 text-emerald-400"
              : "bg-slate-500/10 text-slate-400"
          }`}
        >
          {medication.status}
        </span>
      </div>
      {medication.important && (
        <div className="mt-3 flex items-center gap-2 rounded-lg bg-amber-500/10 px-3 py-2">
          <AlertTriangle className="h-3 w-3 text-amber-400" />
          <span className="text-[11px] text-amber-400">Important — requires remote verification</span>
        </div>
      )}
    </div>
  );
}

export default function MedicationsPage() {
  const { user } = useAuth();
  const [patients, setPatients] = useState([]);
  const [selectedPatientId, setSelectedPatientId] = useState("");
  const [medications, setMedications] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getMyPatients()
      .then((data) => {
        setPatients(data || []);
        if (data?.length === 1) {
          setSelectedPatientId(data[0].id);
          return getPatientMedications(data[0].id);
        }
        return null;
      })
      .then((data) => {
        if (data) setMedications(data || []);
      })
      .catch(() => setMedications([]))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!selectedPatientId) return;
    setLoading(true);
    getPatientMedications(selectedPatientId)
      .then((data) => setMedications(data || []))
      .catch(() => setMedications([]))
      .finally(() => setLoading(false));
  }, [selectedPatientId]);

  return (
    <div className="space-y-6 pb-10">
      <div>
        <p className="text-xs font-medium text-teal-400">Pharmacy</p>
        <h1 className="mt-1 text-xl font-semibold">Medications</h1>
        <p className="mt-1 text-xs text-[var(--text-muted)]">
          Active prescriptions and administration history
        </p>
      </div>

      {patients.length > 1 && (
        <div className="surface rounded-2xl p-4">
          <div className="flex items-center gap-2">
            <Search className="h-4 w-4 text-[var(--text-muted)]" />
            <select
              value={selectedPatientId}
              onChange={(e) => setSelectedPatientId(e.target.value)}
              className="h-10 rounded-xl border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3 text-xs text-[var(--text-primary)] outline-none focus:border-teal-500/50"
            >
              <option value="">Select a patient</option>
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
          Loading medications...
        </div>
      )}

      {!loading && (
        <div className="grid gap-3 sm:grid-cols-2">
          {medications.length === 0 ? (
            <div className="surface rounded-2xl p-8 text-center sm:col-span-2">
              <Pill className="mx-auto h-8 w-8 text-[var(--text-muted)]" />
              <p className="mt-3 text-sm font-medium">No medications found</p>
            </div>
          ) : (
            medications.map((med) => <MedicationCard key={med.id} medication={med} />)
          )}
        </div>
      )}
    </div>
  );
}