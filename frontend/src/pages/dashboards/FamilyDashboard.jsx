import { useNavigate } from "react-router-dom";
import { Bot, HeartHandshake, Users, ChevronRight } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import { useStore } from "../../store/useStore";
import { usePatients } from "../../services/patientHooks";
import { MOCK_PATIENTS } from "../../services/mockDemoData";

export default function FamilyDashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { toggleCopilot } = useStore();
  const { patients: hookPatients } = usePatients();

  const patients = (hookPatients && hookPatients.length > 0) ? hookPatients : MOCK_PATIENTS;
  const caregiverName = user?.fullName || "Sunita Verma";

  return (
    <div className="space-y-6 pb-12">
      <div>
        <p className="text-xs font-semibold uppercase tracking-wider text-teal-400">
          Apollo CareFlow Institute · Family Caregiver Portal
        </p>
        <h1 className="mt-0.5 text-2xl font-bold tracking-tight text-[var(--text-primary)]">
          Welcome, {caregiverName}
        </h1>
        <p className="mt-0.5 text-xs text-[var(--text-secondary)]">
          Secure, verified care updates, medication adherence, and timeline events for your loved ones
        </p>
      </div>

      <div className="surface flex items-center justify-between gap-4 rounded-2xl border border-[var(--border-color)] p-5">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-teal-500/10 text-teal-400">
            <Bot className="h-5 w-5" />
          </div>
          <div className="min-w-0">
            <p className="text-sm font-bold text-[var(--text-primary)]">Clinical Translation Assistant</p>
            <p className="text-xs text-[var(--text-secondary)]">
              Ask CareFlow Copilot to translate medical terminology into clear language.
            </p>
          </div>
        </div>
        <button
          type="button"
          onClick={toggleCopilot}
          className="shrink-0 rounded-xl bg-teal-500 px-3.5 py-2 text-xs font-bold text-slate-950 hover:bg-teal-400 transition"
        >
          Open Copilot
        </button>
      </div>

      <section className="surface rounded-2xl border border-[var(--border-color)] overflow-hidden">
        <div className="flex items-center gap-2 border-b border-[var(--border-color)] px-5 py-4">
          <Users className="h-4 w-4 text-teal-400" />
          <h2 className="text-sm font-bold text-[var(--text-primary)]">
            Monitored Family Members ({patients.length})
          </h2>
        </div>

        <div className="divide-y divide-[var(--border-color)]">
          {patients.map((p) => (
            <button
              key={p.id}
              type="button"
              onClick={() => navigate(`/patients/${p.id}`)}
              className="flex w-full items-center justify-between px-5 py-3.5 text-left transition hover:bg-[var(--bg-subtle)] group"
            >
              <div>
                <p className="text-xs font-semibold text-[var(--text-primary)] group-hover:text-teal-400 transition">
                  {p.name}
                </p>
                <p className="mt-0.5 text-[11px] font-mono text-[var(--text-muted)]">
                  {p.mrn} · Post-CABG recovery · Apollo ICU-204
                </p>
              </div>
              <div className="flex items-center gap-1 text-xs font-semibold text-teal-400">
                <span>View Timeline</span>
                <ChevronRight className="h-3.5 w-3.5 group-hover:translate-x-0.5 transition" />
              </div>
            </button>
          ))}
        </div>
      </section>
    </div>
  );
}
