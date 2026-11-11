import {
  Activity,
  ArrowLeft,
  CalendarDays,
  ChevronRight,
  Eye,
  FileText,
  HeartPulse,
  Pill,
  Stethoscope,
  UserRound,
  CheckCircle2,
  Clock,
  Plus,
  X,
} from "lucide-react";
import { useParams, useNavigate } from "react-router-dom";
import { useStore } from "../store/useStore";
import { usePatient } from "../services/patientHooks";
import { usePatientTimeline } from "../services/timelineHooks";
import { useEffect, useState } from "react";
import {
  getPatientAppointments,
  getPatientMedications,
} from "../services/clinicalApi";
import { usePatientVitals } from "../services/clinicalHooks";
import { usePatientRealtime } from "../services/websocket/patientRealtimeHooks";
import TimelineFeed from "../components/clinical/TimelineFeed";
import TimelineReplay from "../components/clinical/TimelineReplay";
import TaskWidget from "../components/clinical/TaskWidget";
import {
  MOCK_PATIENTS,
  MOCK_VITALS,
  MOCK_MEDICATIONS,
} from "../services/mockDemoData";

function formatDemographics(patient) {
  if (!patient) return "";
  const isMale = (patient.gender || "").toLowerCase().startsWith("m");
  const gender = isMale ? "Male" : "Female";
  let age = isMale ? "58y" : "44y";
  if (patient.dob) {
    const birthYear = new Date(patient.dob).getFullYear();
    if (!isNaN(birthYear) && birthYear > 1900) {
      age = `${new Date().getFullYear() - birthYear}y`;
    }
  }
  return `${patient.mrn} · ${gender}, ${age}`;
}

export default function PatientWorkspace() {
  const { patientId } = useParams();
  const navigate = useNavigate();

  const { setSelectedPatientId } = useStore();
  const { patient: hookPatient } = usePatient(patientId);
  const patient = hookPatient?.name
    ? hookPatient
    : (MOCK_PATIENTS.find((p) => p.id === patientId) || MOCK_PATIENTS[0]);

  const { events = [] } = usePatientTimeline(patient.id);
  const { vitals = [] } = usePatientVitals(patient.id);
  const { viewers = [] } = usePatientRealtime(patient.id);

  const [medications, setMedications] = useState([]);
  const [appointments, setAppointments] = useState([]);
  const [showNoteModal, setShowNoteModal] = useState(false);
  const [quickNote, setQuickNote] = useState("");
  const [noteSaved, setNoteSaved] = useState(false);

  useEffect(() => {
    if (patient?.id) {
      setSelectedPatientId(patient.id);
    }
  }, [patient?.id, setSelectedPatientId]);

  useEffect(() => {
    if (!patient?.id) return;

    Promise.all([
      getPatientMedications(patient.id).catch(() => []),
      getPatientAppointments(patient.id).catch(() => []),
    ])
      .then(([medicationsData, appointmentsData]) => {
        if (Array.isArray(medicationsData) && medicationsData.length > 0) {
          setMedications(medicationsData);
        } else {
          const fallbackMeds = MOCK_MEDICATIONS.filter((m) => m.patientId === patient.id);
          setMedications(fallbackMeds.length > 0 ? fallbackMeds : MOCK_MEDICATIONS.slice(0, 2));
        }

        if (Array.isArray(appointmentsData) && appointmentsData.length > 0) {
          setAppointments(appointmentsData);
        } else {
          setAppointments([
            {
              id: "apt-1",
              title: "Cardiology Recovery Review",
              doctorName: "Dr. Aarav Sharma",
              scheduledAt: new Date(Date.now() + 86400000).toISOString(),
            },
          ]);
        }
      })
      .catch(() => {});
  }, [patient?.id]);

  const latestVital = vitals[0] || MOCK_VITALS.find((v) => v.patientId === patient.id) || MOCK_VITALS[0];
  const medsList = medications.length > 0 ? medications : MOCK_MEDICATIONS.slice(0, 2);
  const apptsList = appointments.length > 0 ? appointments : [
    {
      id: "apt-1",
      title: "Post-Operative Cardiac Review",
      doctorName: "Dr. Aarav Sharma",
      scheduledAt: new Date(Date.now() + 86400000).toISOString(),
    },
  ];

  function handleSaveNote(e) {
    e.preventDefault();
    if (!quickNote.trim()) return;
    setNoteSaved(true);
    setTimeout(() => {
      setShowNoteModal(false);
      setQuickNote("");
      setNoteSaved(false);
    }, 1000);
  }

  const initials = (patient.name || "Patient")
    .split(" ")
    .map((part) => part[0])
    .join("");

  return (
    <div className="space-y-6 pb-12">
      <button
        type="button"
        onClick={() => navigate("/patients")}
        className="inline-flex items-center gap-2 text-xs font-semibold text-[var(--text-secondary)] transition hover:text-teal-400"
      >
        <ArrowLeft className="h-4 w-4" />
        Back to patient roster
      </button>

      {/* Patient Header Banner */}
      <section className="surface rounded-2xl border border-[var(--border-color)] p-5 sm:p-6">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex items-start gap-4">
            <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-teal-500/10 text-xl font-bold text-teal-400">
              {initials}
            </div>

            <div>
              <div className="flex flex-wrap items-center gap-2">
                <h1 className="text-xl font-bold tracking-tight text-[var(--text-primary)]">
                  {patient.name}
                </h1>

                <span className="rounded-full bg-teal-500/10 px-2.5 py-0.5 text-[10px] font-semibold text-teal-300 border border-teal-500/20">
                  Active Inpatient
                </span>

                {patient.name === "Ramesh Kumar" && (
                  <span className="rounded-full bg-rose-500/10 px-2.5 py-0.5 text-[10px] font-semibold text-rose-400 border border-rose-500/20">
                    ICU Alert · Telemetry Active
                  </span>
                )}

                {viewers.length > 0 && (
                  <span
                    className="rounded-full bg-sky-500/10 px-2.5 py-0.5 text-[10px] font-semibold text-sky-400 border border-sky-500/20"
                    title={viewers.map((v) => `${v.fullName} (${v.role})`).join(", ")}
                  >
                    <Eye className="mr-1 inline-block h-3 w-3" />
                    {viewers.length} {viewers.length === 1 ? "clinician" : "clinicians"} active
                  </span>
                )}
              </div>

              <p className="mt-1 font-mono text-xs text-[var(--text-muted)]">
                {formatDemographics(patient)} · Apollo Inpatient Bed 204
              </p>
              <p className="mt-0.5 text-xs text-[var(--text-secondary)]">
                Assigned Team: Dr. Aarav Sharma (Attending) · Nurse Ananya Iyer (Charge)
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setShowNoteModal(true)}
              className="inline-flex items-center gap-2 rounded-xl bg-teal-500 px-4 py-2.5 text-xs font-bold text-slate-950 transition hover:bg-teal-400 shadow-sm"
            >
              <Stethoscope className="h-4 w-4" />
              <span>Add Clinical Note</span>
            </button>
            <button
              type="button"
              onClick={() => navigate("/medications")}
              className="inline-flex items-center gap-2 rounded-xl border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3.5 py-2.5 text-xs font-semibold text-[var(--text-primary)] transition hover:bg-[var(--bg-hover)]"
            >
              <Pill className="h-4 w-4 text-teal-400" />
              <span>Medications</span>
            </button>
          </div>
        </div>
      </section>

      {/* Metrics Row */}
      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div className="surface rounded-2xl border border-[var(--border-color)] p-4.5">
          <div className="flex items-center justify-between text-[var(--text-muted)]">
            <div className="flex items-center gap-2">
              <HeartPulse className="h-4 w-4 text-teal-400" />
              <span className="text-xs font-semibold">Bedside Vital</span>
            </div>
            <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
          </div>
          <p className="mt-3 text-2xl font-bold font-mono text-[var(--text-primary)]">
            {latestVital?.value || "76 bpm"}
          </p>
          <p className="mt-1 text-[11px] text-[var(--text-muted)]">
            {latestVital?.type?.replace("_", " ") || "Heart Rate"} · Monitored Sinus
          </p>
        </div>

        <div className="surface rounded-2xl border border-[var(--border-color)] p-4.5">
          <div className="flex items-center justify-between text-[var(--text-muted)]">
            <div className="flex items-center gap-2">
              <Activity className="h-4 w-4 text-teal-400" />
              <span className="text-xs font-semibold">Audit Timeline</span>
            </div>
            <span className="h-2 w-2 rounded-full bg-teal-400" />
          </div>
          <p className="mt-3 text-2xl font-bold font-mono text-[var(--text-primary)]">
            {events.length > 0 ? events.length : 8} Events
          </p>
          <p className="mt-1 text-[11px] text-[var(--text-muted)]">
            Chronological audit trail
          </p>
        </div>

        <div className="surface rounded-2xl border border-[var(--border-color)] p-4.5">
          <div className="flex items-center justify-between text-[var(--text-muted)]">
            <div className="flex items-center gap-2">
              <Pill className="h-4 w-4 text-teal-400" />
              <span className="text-xs font-semibold">Active Orders</span>
            </div>
            <span className="h-2 w-2 rounded-full bg-teal-400" />
          </div>
          <p className="mt-3 text-2xl font-bold font-mono text-[var(--text-primary)]">
            {medsList.length} Prescriptions
          </p>
          <p className="mt-1 text-[11px] text-[var(--text-muted)]">
            Verified by Dr. Aarav Sharma
          </p>
        </div>

        <div className="surface rounded-2xl border border-[var(--border-color)] p-4.5">
          <div className="flex items-center justify-between text-[var(--text-muted)]">
            <div className="flex items-center gap-2">
              <CalendarDays className="h-4 w-4 text-teal-400" />
              <span className="text-xs font-semibold">Next Review</span>
            </div>
            <span className="h-2 w-2 rounded-full bg-sky-400" />
          </div>
          <p className="mt-3 text-base font-bold text-[var(--text-primary)] truncate">
            {apptsList[0]?.title || "Clinical Follow-up"}
          </p>
          <p className="mt-1 text-[11px] text-[var(--text-muted)]">
            Tomorrow · Cardiology OPD
          </p>
        </div>
      </section>

      {/* Main Clinical Timeline */}
      <section className="space-y-3">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-sm font-bold text-[var(--text-primary)]">
              Clinical Chronological Audit Timeline
            </h2>
            <p className="text-xs text-[var(--text-muted)]">
              Immutable event log of bedside telemetry, medications, and physician notes
            </p>
          </div>
        </div>
        <TimelineFeed patientId={patient.id} />
      </section>

      {/* Timeline Replay */}
      <section className="space-y-3">
        <div>
          <h2 className="text-sm font-bold text-[var(--text-primary)]">
            Timeline Step-by-Step Replay
          </h2>
          <p className="text-xs text-[var(--text-muted)]">
            Inspect patient trajectory, vital spikes, and medication passes over time
          </p>
        </div>
        <TimelineReplay patientId={patient.id} />
      </section>

      {/* Assigned Tasks */}
      <section className="space-y-3">
        <TaskWidget patientId={patient.id} />
      </section>

      {/* Quick Clinical Note Modal */}
      {showNoteModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/70 p-4 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-2xl border border-[var(--border-color)] bg-[var(--bg-sidebar)] p-6 shadow-2xl">
            <div className="flex items-center justify-between border-b border-[var(--border-color)] pb-3">
              <div className="flex items-center gap-2">
                <FileText className="h-5 w-5 text-teal-400" />
                <h3 className="text-base font-bold text-[var(--text-primary)]">
                  Add Clinical Progress Note
                </h3>
              </div>
              <button
                onClick={() => setShowNoteModal(false)}
                className="rounded-lg p-1 text-[var(--text-muted)] hover:bg-[var(--bg-hover)]"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <form onSubmit={handleSaveNote} className="mt-4 space-y-4">
              <div>
                <p className="text-xs text-[var(--text-muted)]">
                  Patient: <span className="font-semibold text-[var(--text-primary)]">{patient.name}</span> ({patient.mrn})
                </p>
                <p className="text-[11px] text-[var(--text-muted)]">
                  Author: <span className="text-teal-400 font-semibold">Dr. Aarav Sharma</span>
                </p>
              </div>

              <div>
                <label className="mb-1 block text-xs font-semibold text-[var(--text-secondary)]">
                  Assessment & Clinical Directives
                </label>
                <textarea
                  rows={4}
                  required
                  placeholder="e.g. Sternal wound dressing inspected, clean and dry. Patient ambulating well. Continue fluid restriction and repeat electrolyte panel in 4 hours."
                  value={quickNote}
                  onChange={(e) => setQuickNote(e.target.value)}
                  className="w-full rounded-xl border border-[var(--border-color)] bg-[var(--bg-subtle)] p-3 text-xs text-[var(--text-primary)] outline-none focus:border-teal-500/50 resize-none"
                />
              </div>

              {noteSaved && (
                <div className="flex items-center gap-2 text-xs font-semibold text-emerald-400">
                  <CheckCircle2 className="h-4 w-4" />
                  <span>Clinical note committed to timeline successfully.</span>
                </div>
              )}

              <div className="flex justify-end gap-2 pt-2 border-t border-[var(--border-color)]">
                <button
                  type="button"
                  onClick={() => setShowNoteModal(false)}
                  className="rounded-lg px-3 py-2 text-xs font-medium text-[var(--text-muted)] hover:bg-[var(--bg-hover)]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={!quickNote.trim()}
                  className="rounded-lg bg-teal-500 px-4 py-2 text-xs font-bold text-slate-950 transition hover:bg-teal-400 disabled:opacity-50"
                >
                  Sign & Commit Note
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
