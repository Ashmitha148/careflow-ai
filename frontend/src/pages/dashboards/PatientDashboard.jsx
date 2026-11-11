import { useEffect, useState } from "react";
import {
  HeartPulse,
  Pill,
  CalendarDays,
  Activity,
  FileText,
  AlertCircle,
  Clock,
  CheckCircle2,
} from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import TimelineFeed from "../../components/clinical/TimelineFeed";
import { getMyPatients } from "../../services/patientApi";
import {
  getPatientVitals,
  getPatientMedications,
  getPatientAppointments,
} from "../../services/clinicalApi";
import {
  MOCK_PATIENTS,
  MOCK_VITALS,
  MOCK_MEDICATIONS,
} from "../../services/mockDemoData";

export default function PatientDashboard() {
  const { user } = useAuth();
  const [patient, setPatient] = useState(MOCK_PATIENTS[0]);
  const [vitals, setVitals] = useState(MOCK_VITALS);
  const [medications, setMedications] = useState(MOCK_MEDICATIONS);
  const [appointments, setAppointments] = useState([
    {
      id: "apt-1",
      title: "Post-Operative Cardiac Follow-up",
      doctorName: "Dr. Aarav Sharma",
      scheduledAt: new Date(Date.now() + 86400000).toISOString(),
    },
  ]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    getMyPatients()
      .then((patients) => {
        if (patients && patients.length > 0) {
          const p = patients[0];
          setPatient(p);
          return Promise.all([
            getPatientVitals(p.id).catch(() => MOCK_VITALS),
            getPatientMedications(p.id).catch(() => MOCK_MEDICATIONS),
            getPatientAppointments(p.id).catch(() => []),
          ]);
        }
        return [MOCK_VITALS, MOCK_MEDICATIONS, []];
      })
      .then(([v, m, a]) => {
        if (v && v.length > 0) setVitals(v);
        if (m && m.length > 0) setMedications(m);
        if (a && a.length > 0) setAppointments(a);
      })
      .catch(() => {});
  }, []);

  const latestVital = vitals[0] || MOCK_VITALS[0];
  const patientDisplayName = patient?.name || user?.fullName || "Ramesh Kumar";

  return (
    <div className="space-y-6 pb-12">
      <div>
        <p className="text-xs font-semibold uppercase tracking-wider text-teal-400">
          Apollo CareFlow Institute · Patient Health Portal
        </p>
        <h1 className="mt-0.5 text-2xl font-bold tracking-tight text-[var(--text-primary)]">
          Welcome, {patientDisplayName}
        </h1>
        <p className="mt-0.5 text-xs text-[var(--text-secondary)]">
          Your personal health record, active prescriptions, and bedside vitals
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <div className="surface rounded-2xl border border-[var(--border-color)] p-5">
          <div className="flex items-center justify-between text-[var(--text-muted)]">
            <div className="flex items-center gap-2">
              <HeartPulse className="h-4 w-4 text-teal-400" />
              <span className="text-xs font-semibold">Latest Vital Signs</span>
            </div>
            <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
          </div>
          <p className="mt-3 text-2xl font-bold font-mono text-[var(--text-primary)]">
            {latestVital?.value || "76 bpm"}
          </p>
          <p className="mt-1 text-xs text-[var(--text-secondary)]">
            {latestVital?.type?.replace("_", " ") || "Heart Rate"} · Normal Sinus
          </p>
        </div>

        <div className="surface rounded-2xl border border-[var(--border-color)] p-5">
          <div className="flex items-center justify-between text-[var(--text-muted)]">
            <div className="flex items-center gap-2">
              <Pill className="h-4 w-4 text-teal-400" />
              <span className="text-xs font-semibold">Active Medications</span>
            </div>
            <span className="h-2 w-2 rounded-full bg-teal-400" />
          </div>
          <p className="mt-3 text-2xl font-bold font-mono text-[var(--text-primary)]">
            {medications.length} Prescriptions
          </p>
          <p className="mt-1 text-xs text-[var(--text-secondary)]">
            Prescribed by Dr. Aarav Sharma
          </p>
        </div>

        <div className="surface rounded-2xl border border-[var(--border-color)] p-5">
          <div className="flex items-center justify-between text-[var(--text-muted)]">
            <div className="flex items-center gap-2">
              <CalendarDays className="h-4 w-4 text-teal-400" />
              <span className="text-xs font-semibold">Next Clinical Review</span>
            </div>
            <span className="h-2 w-2 rounded-full bg-sky-400" />
          </div>
          <p className="mt-3 text-lg font-bold text-[var(--text-primary)] truncate">
            {appointments[0]?.title || "Cardiology Review"}
          </p>
          <p className="mt-1 text-xs text-[var(--text-secondary)]">
            {appointments[0]?.doctorName || "Dr. Aarav Sharma"} · Apollo Ward
          </p>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <section className="surface rounded-2xl border border-[var(--border-color)] p-5 space-y-3">
          <h2 className="text-sm font-bold text-[var(--text-primary)] border-b border-[var(--border-color)] pb-2">
            My Active Medications
          </h2>
          <div className="space-y-2.5">
            {medications.map((m) => (
              <div key={m.id} className="flex items-start justify-between rounded-xl border border-[var(--border-color)] bg-[var(--bg-subtle)] p-3">
                <div>
                  <p className="text-xs font-semibold text-[var(--text-primary)]">{m.name}</p>
                  <p className="text-[11px] text-[var(--text-secondary)]">{m.dosage} · {m.frequency}</p>
                  <p className="mt-1 text-[10px] text-[var(--text-muted)]">Prescribed by: {m.prescribedBy || "Dr. Aarav Sharma"}</p>
                </div>
                <span className="rounded-full bg-teal-500/10 px-2 py-0.5 text-[10px] font-semibold text-teal-300">
                  {m.status || "Active"}
                </span>
              </div>
            ))}
          </div>
        </section>

        <section className="surface rounded-2xl border border-[var(--border-color)] p-5 space-y-3">
          <h2 className="text-sm font-bold text-[var(--text-primary)] border-b border-[var(--border-color)] pb-2">
            Care Timeline
          </h2>
          {patient?.id && <TimelineFeed patientId={patient.id} />}
        </section>
      </div>
    </div>
  );
}
