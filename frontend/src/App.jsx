import { Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import ProtectedRoute from "./components/auth/ProtectedRoute";
import AppShell from "./components/layout/AppShell";
import Login from "./pages/Login";
import RoleDashboard from "./pages/RoleDashboard";
import Overview from "./pages/Overview";
import PatientList from "./pages/PatientList";
import PatientWorkspace from "./pages/PatientWorkspace";
import TimelinePage from "./pages/TimelinePage";
import MedicationsPage from "./pages/MedicationsPage";
import TasksPage from "./pages/TasksPage";
import ShiftHandoffsPage from "./pages/ShiftHandoffsPage";
import AuditPanel from "./pages/dashboards/AuditPanel";
import NurseDashboard from "./pages/dashboards/NurseDashboard";
import FamilyDashboard from "./pages/dashboards/FamilyDashboard";
import PatientDashboard from "./pages/dashboards/PatientDashboard";
import VideoVerificationPage from "./pages/VideoVerificationPage";

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AppShell />}>
            {/* Role-based default landing */}
            <Route path="/" element={<RoleDashboard />} />

            {/* Clinical staff routes */}
            <Route path="/overview" element={<Overview />} />
            <Route path="/patients" element={<PatientList />} />
            <Route path="/patients/:patientId" element={<PatientWorkspace />} />
            <Route path="/timeline" element={<TimelinePage />} />
            <Route path="/medications" element={<MedicationsPage />} />
            <Route path="/tasks" element={<TasksPage />} />
            <Route path="/handoffs" element={<ShiftHandoffsPage />} />
            <Route path="/nurse-dashboard" element={<NurseDashboard />} />

            {/* Family/Caregiver routes */}
            <Route path="/family" element={<FamilyDashboard />} />

            {/* Patient routes */}
            <Route path="/my-care" element={<PatientDashboard />} />
            <Route
              path="/verify-medication"
              element={<VideoVerificationPage />}
            />

            {/* Admin routes */}
            <Route path="/audit" element={<AuditPanel />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  );
}
