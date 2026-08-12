import { useNavigate } from "react-router-dom";
import { Bot, HeartHandshake, Users } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import { useStore } from "../../store/useStore";
import { usePatients } from "../../services/patientHooks";

export default function FamilyDashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { toggleCopilot } = useStore();
  const { patients, loading } = usePatients();

  return (
    <div className="space-y-6 pb-10">
      <div>
        <p className="text-xs font-medium text-teal-400">
          {user?.role === "CAREGIVER" ? "Caregiver workspace" : "Family workspace"}
        </p>
        <h1 className="mt-1 text-xl font-semibold">
          Welcome, {user?.fullName?.split(" ")[0] || "there"}
        </h1>
        <p className="mt-1 text-xs text-[var(--text-muted)]">
          A read-only view of care activity and history
        </p>
      </div>

      <div className="surface flex items-center gap-4 rounded-2xl p-5">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-teal-500/10 text-teal-400">
          <Bot className="h-5 w-5" />
        </div>
        <div className="min-w-0 flex-1">
          <p className="text-sm font-medium">Not sure what a note means?</p>
          <p className="mt-0.5 text-xs text-[var(--text-muted)]">
            Ask CareFlow Copilot to explain any clinical entry in plain language.
          </p>
        </div>
        <button
          type="button"
          onClick={toggleCopilot}
          className="shrink-0 rounded-xl bg-teal-500 px-3 py-2 text-xs font-semibold text-slate-950 hover:bg-teal-400"
        >
          Open Copilot
        </button>
      </div>

      <section className="surface overflow-hidden rounded-2xl">
        <div className="flex items-center gap-2 border-b border-[var(--border-color)] px-5 py-4">
          <Users className="h-4 w-4 text-teal-400" />
          <h2 className="text-sm font-semibold">Patients</h2>
        </div>

        {loading && (
          <div className="px-5 py-8 text-center text-sm text-[var(--text-muted)]">Loading...</div>
        )}

        {!loading && patients.length === 0 && (
          <div className="px-5 py-10 text-center">
            <HeartHandshake className="mx-auto h-6 w-6 text-[var(--text-muted)]" />
            <p className="mt-3 text-sm font-medium">No patient records available</p>
          </div>
        )}

        {!loading && patients.length > 0 && (
          <div className="divide-y divide-[var(--border-color)]">
            {patients.map((p) => (
              <button
                key={p.id}
                type="button"
                onClick={() => navigate(`/patients/${p.id}`)}
                className="flex w-full items-center justify-between px-5 py-3 text-left transition hover:bg-[var(--bg-subtle)]"
              >
                <div>
                  <p className="text-xs font-medium">{p.name}</p>
                  <p className="mt-0.5 text-[11px] text-[var(--text-muted)]">{p.mrn}</p>
                </div>
                <span className="text-[11px] text-teal-400">View timeline</span>
              </button>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
