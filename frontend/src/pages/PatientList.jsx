import { usePatients } from "../services/patientHooks";

export default function PatientList() {
  const { patients, loading, error } = usePatients();

  if (loading) {
    return <div className="p-6 text-sm text-slate-400">Loading patients...</div>;
  }

  if (error) {
    return <div className="p-6 text-sm text-rose-400">Unable to load patients.</div>;
  }

  return (
    <div className="space-y-3">
      {patients.map((patient) => (
        <div
          key={patient.id}
          className="rounded-xl border border-slate-800 bg-slate-900/60 p-4"
        >
          <p className="font-medium text-slate-100">{patient.name}</p>
          <p className="mt-1 text-xs text-slate-400">{patient.mrn}</p>
        </div>
      ))}
    </div>
  );
}
