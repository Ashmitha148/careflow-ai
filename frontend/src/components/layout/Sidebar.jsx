import {
  Activity,
  ClipboardList,
  FileClock,
  LayoutDashboard,
  Pill,
  ShieldCheck,
  Users,
  ArrowRightLeft,
  LogOut,
  Home,
  Video,
} from "lucide-react";
import { useNavigate, useLocation } from "react-router-dom";
import { useStore } from "../../store/useStore";
import { useAuth } from "../../context/AuthContext";

const getNavigation = (role) => {
  switch (role) {
    case "PATIENT":
      return [
        { id: "my-care", label: "My Care", icon: Home, path: "/my-care" },
        {
          id: "verify-medication",
          label: "Verify Medication",
          icon: Video,
          path: "/verify-medication",
        },
      ];
    case "CAREGIVER":
    case "READ_ONLY":
      return [
        { id: "family", label: "My Patients", icon: Users, path: "/family" },
        {
          id: "timeline",
          label: "Timeline",
          icon: FileClock,
          path: "/timeline",
        },
      ];
    case "NURSE":
      return [
        {
          id: "nurse-dashboard",
          label: "Dashboard",
          icon: LayoutDashboard,
          path: "/nurse-dashboard",
        },
        { id: "patients", label: "Patients", icon: Users, path: "/patients" },
        {
          id: "timeline",
          label: "Timeline",
          icon: FileClock,
          path: "/timeline",
        },
        {
          id: "medications",
          label: "Medications",
          icon: Pill,
          path: "/medications",
        },
        { id: "tasks", label: "Tasks", icon: ClipboardList, path: "/tasks" },
        {
          id: "handoffs",
          label: "Shift handoffs",
          icon: ArrowRightLeft,
          path: "/handoffs",
        },
      ];
    case "DOCTOR":
    case "ADMIN":
      return [
        {
          id: "overview",
          label: "Overview",
          icon: LayoutDashboard,
          path: "/overview",
        },
        { id: "patients", label: "Patients", icon: Users, path: "/patients" },
        {
          id: "timeline",
          label: "Timeline",
          icon: FileClock,
          path: "/timeline",
        },
        {
          id: "medications",
          label: "Medications",
          icon: Pill,
          path: "/medications",
        },
        { id: "tasks", label: "Tasks", icon: ClipboardList, path: "/tasks" },
        {
          id: "handoffs",
          label: "Shift handoffs",
          icon: ArrowRightLeft,
          path: "/handoffs",
        },
      ];
    default:
      return [];
  }
};

export default function Sidebar() {
  const navigate = useNavigate();
  const location = useLocation();
  const { setActiveTab, activeTab } = useStore();
  const { user, logout, isAdmin } = useAuth();

  const navigation = getNavigation(user?.role);
  const currentPath = location.pathname;

  const getActiveId = () => {
    const match = navigation.find((n) => currentPath.startsWith(n.path));
    return match?.id || activeTab;
  };

  const activeId = getActiveId();

  const handleNavClick = (id, path) => {
    setActiveTab(id);
    navigate(path);
  };

  return (
    <aside className="fixed inset-y-0 left-0 z-40 hidden w-[248px] border-r border-[var(--border-color)] bg-[var(--bg-sidebar)] lg:flex lg:flex-col">
      <div className="flex h-[72px] items-center border-b border-[var(--border-color)] px-6">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-teal-500 text-slate-950 shadow-lg shadow-teal-500/10">
            <Activity className="h-5 w-5" strokeWidth={2.2} />
          </div>
          <div>
            <div className="text-[15px] font-semibold tracking-tight">
              CareFlow
            </div>
            <div className="text-[10px] font-medium uppercase tracking-[0.16em] text-[var(--text-muted)]">
              Clinical continuity
            </div>
          </div>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto px-3 py-5">
        <p className="px-3 pb-2 text-[10px] font-semibold uppercase tracking-[0.16em] text-[var(--text-muted)]">
          Workspace
        </p>
        <nav className="space-y-1">
          {navigation.map(({ id, label, icon: Icon, path }) => {
            const active = activeId === id;
            return (
              <button
                key={id}
                type="button"
                onClick={() => handleNavClick(id, path)}
                className={[
                  "group flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm transition-all duration-200",
                  active
                    ? "bg-teal-500/10 text-teal-400"
                    : "text-[var(--text-secondary)] hover:bg-[var(--bg-hover)] hover:text-[var(--text-primary)]",
                ].join(" ")}
              >
                <Icon
                  className={[
                    "h-[17px] w-[17px] transition-colors",
                    active
                      ? "text-teal-400"
                      : "text-[var(--text-muted)] group-hover:text-[var(--text-primary)]",
                  ].join(" ")}
                  strokeWidth={1.8}
                />
                <span>{label}</span>
                {active && (
                  <span className="ml-auto h-1.5 w-1.5 rounded-full bg-teal-400" />
                )}
              </button>
            );
          })}
        </nav>

        <div className="my-6 border-t border-[var(--border-color)]" />
        <p className="px-3 pb-2 text-[10px] font-semibold uppercase tracking-[0.16em] text-[var(--text-muted)]">
          Intelligence
        </p>

        <button
          type="button"
          onClick={() => setActiveTab("copilot")}
          className="group flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm transition-all duration-200 text-[var(--text-secondary)] hover:bg-[var(--bg-hover)] hover:text-[var(--text-primary)]"
        >
          <div className="flex h-[17px] w-[17px] items-center justify-center">
            <span className="h-2 w-2 rounded-full bg-teal-400 shadow-[0_0_10px_rgba(45,212,191,0.45)]" />
          </div>
          <span>CareFlow Copilot</span>
        </button>

        {isAdmin && (
          <button
            type="button"
            onClick={() => handleNavClick("audit", "/audit")}
            className={[
              "mt-1 flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm transition-all duration-200",
              activeId === "audit"
                ? "bg-teal-500/10 text-teal-400"
                : "text-[var(--text-secondary)] hover:bg-[var(--bg-hover)] hover:text-[var(--text-primary)]",
            ].join(" ")}
          >
            <ShieldCheck className="h-[17px] w-[17px]" strokeWidth={1.8} />
            <span>Audit trail</span>
          </button>
        )}
      </div>

      <div className="border-t border-[var(--border-color)] p-4">
        <div className="flex items-center gap-3 rounded-xl bg-[var(--bg-subtle)] px-3 py-3">
          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-teal-500/10 text-sm font-semibold text-teal-400">
            {user?.fullName?.charAt(0) ?? "C"}
          </div>
          <div className="min-w-0">
            <p className="truncate text-xs font-semibold text-[var(--text-primary)]">
              {user?.fullName ?? "CareFlow user"}
            </p>
            <p className="mt-0.5 text-[10px] text-[var(--text-muted)]">
              {user?.role ?? "USER"}
            </p>
          </div>
          <button
            type="button"
            onClick={logout}
            title="Log out"
            className="ml-auto rounded-lg p-2 text-[var(--text-muted)] transition hover:bg-[var(--bg-hover)] hover:text-[var(--text-primary)]"
          >
            <LogOut className="h-4 w-4" strokeWidth={1.8} />
          </button>
        </div>
      </div>
    </aside>
  );
}
