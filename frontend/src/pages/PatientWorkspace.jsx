import {
  Activity,
  ArrowLeft,
  CalendarDays,
  ChevronRight,
  FileText,
  HeartPulse,
  Pill,
  Stethoscope,
  UserRound,
} from "lucide-react";
import { useParams, useNavigate } from "react-router-dom";
import { useStore } from "../store/useStore";
import { usePatient } from "../services/patientHooks";
import { usePatientTimeline } from "../services/timelineHooks";
import { useEffect, useState } from "react";
import {
  getPatientAppointments,
  getPatientMedications,
  getPatientVitals,
} from "../services/clinicalApi";
import TimelineFeed from "../components/clinical/TimelineFeed";
import TimelineReplay from "../components/clinical/TimelineReplay";
import TaskWidget from "../components/clinical/TaskWidget";

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

function EventIcon({ type }) {
  const Icon = eventIcons[type] || Activity;

  return <Icon className="h-4 w-4" />;
}

function formatEventType(type) {
  return type
    ?.toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function formatDate(value) {
  if (!value) return "Unknown time";

  return new Date(value).toLocaleString([], {
    dateStyle: "medium",
    timeStyle: "short",
  });
}

export default function PatientWorkspace() {
  const { patientId } = useParams();
  const navigate = useNavigate();

  const { setActiveTab, setSelectedPatientId } = useStore();
  const { patient, loading, error } = usePatient(patientId);
  const {
    events,
    loading: timelineLoading,
    error: timelineError,
  } = usePatientTimeline(patientId);

  const [vitals, setVitals] = useState([]);
  const [medications, setMedications] = useState([]);
  const [appointments, setAppointments] = useState([]);

  useEffect(() => {
    if (patient?.id) {
      setSelectedPatientId(patient.id);
    }
  }, [patient?.id, setSelectedPatientId]);

  useEffect(() => {
    if (!patientId) return;

    Promise.all([
      getPatientVitals(patientId),
      getPatientMedications(patientId),
      getPatientAppointments(patientId),
    ])
      .then(([vitalsData, medicationsData, appointmentsData]) => {
        setVitals(vitalsData || []);
        setMedications(medicationsData || []);
        setAppointments(appointmentsData || []);
      })
      .catch(() => {
        setVitals([]);
        setMedications([]);
        setAppointments([]);
      });
  }, [patientId]);

  if (loading) {
    return (
      <div className="surface rounded-2xl p-8 text-center text-sm text-[var(--text-muted)]">
        Loading patient record...
      </div>
    );
  }

  if (error || !patient) {
    return (
      <div className="surface rounded-2xl p-8 text-center">
        <p className="text-sm font-medium">Patient record unavailable</p>

        <button
          type="button"
          onClick={() => navigate("/patients")}
          className="mt-4 rounded-xl bg-teal-500 px-4 py-2 text-xs font-medium text-slate-950"
        >
          Back to patients
        </button>
      </div>
    );
  }

  const latestVital = vitals[0];

  return (
    <div className="space-y-6 pb-10">
      <button
        type="button"
        onClick={() => navigate("/patients")}
        className="inline-flex items-center gap-2 text-xs font-medium text-[var(--text-secondary)] transition hover:text-teal-500"
      >
        <ArrowLeft className="h-4 w-4" />
        Back to patients
      </button>

      <section className="surface rounded-2xl p-5 sm:p-6">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex items-start gap-4">
            <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-teal-500/10 text-lg font-semibold text-teal-400">
              {patient.name
                ?.split(" ")
                .map((part) => part[0])
                .join("")}
            </div>

            <div>
              <div className="flex flex-wrap items-center gap-2">
                <h1 className="text-xl font-semibold tracking-tight">
                  {patient.name}
                </h1>

                <span className="rounded-full bg-teal-500/10 px-2.5 py-1 text-[10px] font-medium text-teal-400">
                  Active record
                </span>
              </div>

              <p className="mt-1 text-xs text-[var(--text-muted)]">
                {patient.mrn}
                {patient.dob ? ` · DOB ${patient.dob}` : ""}
                {patient.gender ? ` · ${patient.gender}` : ""}
              </p>
            </div>
          </div>

          <button
            type="button"
            className="inline-flex items-center gap-2 rounded-xl bg-teal-500 px-3.5 py-2 text-xs font-medium text-slate-950 transition hover:bg-teal-400"
          >
            <Stethoscope className="h-4 w-4" />
            Review patient
          </button>
        </div>
      </section>

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
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
            <Activity className="h-4 w-4" />
            <span className="text-xs">Timeline events</span>
          </div>

          <p className="mt-3 text-2xl font-semibold">{events.length}</p>

          <p className="mt-1 text-[11px] text-[var(--text-muted)]">
            Recorded clinical events
          </p>
        </div>

        <div className="surface rounded-2xl p-4">
          <div className="flex items-center gap-2 text-[var(--text-muted)]">
            <Pill className="h-4 w-4" />
            <span className="text-xs">Medications</span>
          </div>

          <p className="mt-3 text-2xl font-semibold">
            {medications.length}
          </p>
        </div>

        <div className="surface rounded-2xl p-4">
          <div className="flex items-center gap-2 text-[var(--text-muted)]">
            <CalendarDays className="h-4 w-4" />
            <span className="text-xs">Appointments</span>
          </div>

          <p className="mt-3 text-2xl font-semibold">
            {appointments.length}
          </p>
        </div>
      </section>

      <section>
        <div className="mb-3 flex items-center justify-between">
          <div>
            <h2 className="text-sm font-semibold">Clinical timeline</h2>
            <p className="mt-1 text-xs text-[var(--text-muted)]">
              A chronological record of care activity
            </p>
          </div>
        </div>
        <TimelineFeed patientId={patient.id} />
      </section>

      <section>
        <h2 className="mb-3 text-sm font-semibold">Timeline replay</h2>
        <TimelineReplay patientId={patient.id} />
      </section>

      <section>
        <TaskWidget patientId={patient.id} />
      </section>
    </div>
  );
}
