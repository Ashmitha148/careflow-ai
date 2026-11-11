import { useState } from "react";
import { usePatients } from "../services/patientHooks";
import { useNavigate } from "react-router-dom";
import { Search, ChevronRight, Activity, HeartPulse } from "lucide-react";
import { MOCK_PATIENTS } from "../services/mockDemoData";
import { useStore } from "../store/useStore";

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

export default function PatientList() {
  const navigate = useNavigate();
  const { setSelectedPatientId } = useStore();
  const { patients: hookPatients } = usePatients();
  const [search, setSearch] = useState("");

  const patients = (hookPatients && hookPatients.length > 0) ? hookPatients : MOCK_PATIENTS;

  const filtered = patients.filter((p) =>
    (p.name || "").toLowerCase().includes(search.toLowerCase()) ||
    (p.mrn || "").toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-6 pb-12">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider text-teal-400">Inpatient Directory</p>
          <h1 className="mt-0.5 text-2xl font-bold tracking-tight text-[var(--text-primary)]">
            Patients & Bed Allocations
          </h1>
          <p className="text-xs text-[var(--text-secondary)]">
            Apollo CareFlow Institute · Active census, assigned care teams, and monitored telemetry
          </p>
        </div>

        {/* Search */}
        <div className="relative w-full max-w-xs">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-[var(--text-muted)]" />
          <input
            type="text"
            placeholder="Search by patient name or MRN..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full rounded-xl border border-[var(--border-color)] bg-[var(--bg-subtle)] pl-9 pr-3 py-2 text-xs text-[var(--text-primary)] outline-none focus:border-teal-500/50"
          />
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        {filtered.map((patient) => {
          const initials = (patient.name || "Patient")
            .split(" ")
            .map((n) => n[0])
            .join("");
          const isICU = patient.mrn?.includes("ICU") || patient.name === "Ramesh Kumar";

          return (
            <div
              key={patient.id}
              onClick={() => {
                setSelectedPatientId(patient.id);
                navigate(`/patients/${patient.id}`);
              }}
              className="surface rounded-2xl border border-[var(--border-color)] p-5 transition hover:border-teal-500/40 hover:bg-[var(--bg-subtle)] cursor-pointer group"
            >
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-teal-500/10 text-base font-bold text-teal-400 group-hover:bg-teal-500 group-hover:text-slate-950 transition">
                    {initials}
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <p className="text-sm font-bold text-[var(--text-primary)]">
                        {patient.name}
                      </p>
                      {isICU && (
                        <span className="rounded-full bg-rose-500/10 border border-rose-500/20 px-2 py-0.5 text-[10px] font-semibold text-rose-400">
                          ICU Alert
                        </span>
                      )}
                    </div>
                    <p className="text-xs font-mono text-[var(--text-muted)]">
                      {formatDemographics(patient)}
                    </p>
                  </div>
                </div>

                <ChevronRight className="h-4 w-4 text-[var(--text-muted)] group-hover:translate-x-1 group-hover:text-teal-400 transition" />
              </div>

              <div className="mt-4 pt-3 border-t border-[var(--border-color)] flex items-center justify-between text-xs text-[var(--text-secondary)]">
                <div>
                  <span className="text-[10px] uppercase text-[var(--text-muted)] block">Assigned Doctor</span>
                  <span className="font-semibold text-[var(--text-primary)]">
                    {patient.assignedDoctorName || "Dr. Aarav Sharma"}
                  </span>
                </div>
                <div>
                  <span className="text-[10px] uppercase text-[var(--text-muted)] block">Primary Nurse</span>
                  <span className="font-semibold text-[var(--text-primary)]">
                    {patient.assignedNurseName || "Nurse Ananya Iyer"}
                  </span>
                </div>
                <div className="text-right">
                  <span className="text-[10px] uppercase text-[var(--text-muted)] block">Status</span>
                  <span className="font-semibold text-emerald-400">Monitored</span>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
