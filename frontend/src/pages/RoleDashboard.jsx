import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function RoleDashboard() {
  const { user, loading } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (!loading && user) {
      switch (user.role) {
        case "PATIENT":
          navigate("/my-care", { replace: true });
          break;
        case "CAREGIVER":
        case "READ_ONLY":
          navigate("/family", { replace: true });
          break;
        case "NURSE":
          navigate("/nurse-dashboard", { replace: true });
          break;
        case "ADMIN":
          navigate("/audit", { replace: true });
          break;
        default:
          navigate("/overview", { replace: true });
      }
    }
  }, [user, loading, navigate]);

  return (
    <div className="flex h-screen items-center justify-center">
      <div className="animate-spin h-8 w-8 border-2 border-teal-500 border-t-transparent rounded-full" />
    </div>
  );
}
