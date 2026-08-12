import { Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import ProtectedRoute from "./components/auth/ProtectedRoute";
import AppShell from "./components/layout/AppShell";
import Login from "./pages/Login";
import RoleDashboard from "./pages/RoleDashboard";
import PatientWorkspace from "./pages/PatientWorkspace";

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
          <Route path="/" element={<RoleDashboard />} />
          <Route path="/patients" element={<PatientWorkspace />} />
          <Route path="/patients/:patientId" element={<PatientWorkspace />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  );
}
