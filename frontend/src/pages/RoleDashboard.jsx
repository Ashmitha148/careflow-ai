import { useAuth } from "../context/AuthContext";
import Overview from "./Overview";
import NurseDashboard from "./dashboards/NurseDashboard";
import FamilyDashboard from "./dashboards/FamilyDashboard";
import AuditPanel from "./dashboards/AuditPanel";

export default function RoleDashboard() {
  const { user } = useAuth();

  if (user?.role === "NURSE") {
    return <NurseDashboard />;
  }

  if (user?.role === "CAREGIVER" || user?.role === "READ_ONLY") {
    return <FamilyDashboard />;
  }

  if (user?.role === "ADMIN") {
    return (
      <div className="space-y-6 pb-10">
        <Overview />
        <AuditPanel />
      </div>
    );
  }

  // DOCTOR and any unrecognized role fall back to the general overview.
  return <Overview />;
}
