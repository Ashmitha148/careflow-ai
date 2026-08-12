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

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route
          element={
            <ProtectedRoute>
              <AppShell />
            </ProtectedRoute>
          }
        >
          {/* Overview - default landing */}
          <Route path="/" element={<RoleDashboard />} />
          <Route path="/overview" element={<Overview />} />

          {/* Patients */}
          <Route path="/patients" element={<PatientList />} />
          <Route path="/patients/:patientId" element={<PatientWorkspace />} />

          {/* Timeline */}
          <Route path="/timeline" element={<TimelinePage />} />

          {/* Medications */}
          <Route path="/medications" element={<MedicationsPage />} />

          {/* Tasks */}
          <Route path="/tasks" element={<TasksPage />} />

          {/* Shift Handoffs */}
          <Route path="/handoffs" element={<ShiftHandoffsPage />} />

          {/* Admin Audit */}
          <Route path="/audit" element={<AuditPanel />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  );
}
