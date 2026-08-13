import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

export default function ProtectedRoute() {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center bg-[var(--bg-primary)]">
        <div className="animate-spin h-8 w-8 border-2 border-teal-500 border-t-transparent rounded-full" />
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  const { role } = user;
  const path = location.pathname;

  // PATIENT: only /my-care and /verify-medication
  if (role === "PATIENT") {
    if (path !== "/my-care" && path !== "/verify-medication") {
      return <Navigate to="/my-care" replace />;
    }
  }

  // CAREGIVER/READ_ONLY: only /family and /timeline
  if (role === "CAREGIVER" || role === "READ_ONLY") {
    if (path !== "/family" && path !== "/timeline") {
      return <Navigate to="/family" replace />;
    }
  }

  // Only ADMIN can access /audit
  if (path === "/audit" && role !== "ADMIN") {
    return <Navigate to="/overview" replace />;
  }

  return <Outlet />;
}
