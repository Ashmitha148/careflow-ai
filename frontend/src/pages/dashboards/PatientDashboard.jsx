import { useEffect, useState } from "react";
import {
  HeartPulse,
  Pill,
  CalendarDays,
  Activity,
  FileText,
  AlertCircle,
} from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import TimelineFeed from "../../components/clinical/TimelineFeed";

const API_BASE = "/api";

export default function PatientDashboard() {
  const { user } = useAuth();
  const [patient, setPatient] = useState(null);
  const [vitals, setVitals] = useState([]);
  const [medications, setMedications] = useState([]);
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const token = localStorage.getItem("careflow_token");

    // Get patient's own record
    fetch(`${API_BASE}/patients/my`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => r.json())
      .then((patients) => {
        if (patients.length > 0) {
          const p = patients[0];
          setPatient(p);
          return Promise.all([
            fetch(`${API_BASE}/clinical/vitals/${p.id}`, {
              headers: { Authorization: `Bearer ${token}` },
            }).then((r) => r.json()),
            fetch(`${API_BASE}/clinical/medications/${p.id}`, {
              headers: { Authorization: `Bearer ${token}` },
            }).then((r) => r.json()),
            fetch(`${API_BASE}/clinical/appointments/${p.id}`, {
              headers: { Authorization: `Bearer ${token}` },
            }).then((r) => r.json()),
          ]);
        }
        throw new Error("No patient record found");
      })
      .then(([v, m, a]) => {
        setVitals(v || []);
        setMedications(m || []);
        setAppointments(a || []);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="animate-spin h-8 w-8 border-2 border-teal-500 border-t-transparent rounded-full" />
      </div>
    );
  }

  if (error || !patient) {
    return (
      <div className="surface rounded-2xl p-8 text-center">
        <AlertCircle className="mx-auto h-8 w-8 text-rose-400" />
        <p className="mt-3 text-sm font-medium">
          Unable to load your care record
        </p>
        <p className="mt-1 text-xs text-[var(--text-muted)]">{error}</p>
      </div>
    );
  }

  const latestVital = vitals[0];
  const importantMeds = medications.filter((m) => m.important);

  return (
    <div className="space-y-6 pb-10">
      <div>
        <p className="text-xs font-medium text-teal-400">My Health</p>
        <h1 className="mt-1 text-xl font-semibold">Welcome, {patient.name}</h1>
        <p className="mt-1 text-xs text-[var(--text-muted)]">
          Your personal care dashboard
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div className="surface rounded-2xl p-4">
          <div className="flex items-center gap-2 text-[var(--text-muted)]">
            <HeartPulse className="h-4 w-4" />
            <span className="text-xs">Latest vital</span>
          </div>
          <p className="mt-3 text-2xl font-semibold">
            {latestVital?.value ?? "—"}
          </p>
          <p className="mt-1 text-[11px] text-[var(--text-muted)]">
            {latestVital?.type ?? "No observation"}
          </p>
        </div>
        <div className="surface rounded-2xl p-4">
          <div className="flex items-center gap-2 text-[var(--text-muted)]">
            <Pill className="h-4 w-4" />
            <span className="text-xs">Medications</span>
          </div>
          <p className="mt-3 text-2xl font-semibold">{medications.length}</p>
          {importantMeds.length > 0 && (
            <p className="mt-1 text-[11px] text-amber-400">
              {importantMeds.length} require verification
            </p>
          )}
        </div>
        <div className="surface rounded-2xl p-4">
          <div className="flex items-center gap-2 text-[var(--text-muted)]">
            <CalendarDays className="h-4 w-4" />
            <span className="text-xs">Appointments</span>
          </div>
          <p className="mt-3 text-2xl font-semibold">{appointments.length}</p>
        </div>
        <div className="surface rounded-2xl p-4">
          <div className="flex items-center gap-2 text-[var(--text-muted)]">
            <Activity className="h-4 w-4" />
            <span className="text-xs">Timeline events</span>
          </div>
          <p className="mt-3 text-2xl font-semibold">...</p>
        </div>
      </div>

      {importantMeds.length > 0 && (
        <div className="surface rounded-2xl p-5 border border-amber-500/20">
          <div className="flex items-center gap-2 mb-3">
            <AlertCircle className="h-4 w-4 text-amber-400" />
            <h2 className="text-sm font-semibold">Important Medications</h2>
          </div>
          <div className="space-y-2">
            {importantMeds.map((med) => (
              <div
                key={med.id}
                className="flex items-center justify-between rounded-xl bg-amber-500/5 px-3 py-2"
              >
                <div>
                  <p className="text-xs font-medium">{med.name}</p>
                  <p className="text-[11px] text-[var(--text-muted)]">
                    {med.dosage} · {med.frequency}
                  </p>
                </div>
                <span className="rounded-full bg-amber-500/10 px-2 py-0.5 text-[10px] text-amber-400">
                  Verification required
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      <section>
        <h2 className="mb-3 text-sm font-semibold">My Timeline</h2>
        <TimelineFeed patientId={patient.id} />
      </section>
    </div>
  );
}
