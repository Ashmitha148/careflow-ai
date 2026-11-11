import {
  Bell,
  Bot,
  Menu,
  Moon,
  Search,
  Sun,
  User,
  LogOut,
  ChevronDown,
  Clock,
  Pill,
  ArrowRightLeft,
  Users,
  ShieldCheck,
  Building2,
  Sparkles,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useStore } from "../../store/useStore";
import { useAuth } from "../../context/AuthContext";
import { useUnreadCount } from "../../services/notificationsHooks";

function getClinicianName(user) {
  const name = user?.fullName || "";
  if (!name || name.includes("Sarah Jenkins") || user?.email === "doctor1@careflow.ai") {
    return "Dr. Aarav Sharma";
  }
  if (name.includes("Michael Chen") || user?.email === "doctor2@careflow.ai") {
    return "Dr. Priya Patel";
  }
  if (name.includes("Emily Watson") || user?.email === "nurse1@careflow.ai") {
    return "Nurse Ananya Iyer";
  }
  if (name.includes("James Rivera") || user?.email === "nurse2@careflow.ai") {
    return "Nurse Vikram Singh";
  }
  if (name.includes("Maria Rodriguez") || user?.email === "caregiver@careflow.ai") {
    return "Sunita Verma";
  }
  if (name.includes("Eleanor Vance") || user?.email === "patient@careflow.ai") {
    return "Ramesh Kumar";
  }
  return name;
}

export default function Topbar() {
  const navigate = useNavigate();
  const {
    toggleCopilot,
    isCopilotOpen,
    theme,
    toggleTheme,
    unreadNotifications,
    latestNotification,
    clearLatestNotification,
    toggleMobileNav,
  } = useStore();
  const { user: currentUser, logout } = useAuth();
  const [isProfileOpen, setIsProfileOpen] = useState(false);
  const dropdownRef = useRef(null);

  useUnreadCount(currentUser?.id);

  useEffect(() => {
    if (!latestNotification) return undefined;
    const timer = setTimeout(clearLatestNotification, 6000);
    return () => clearTimeout(timer);
  }, [latestNotification, clearLatestNotification]);

  // Close profile dropdown on click outside or escape key
  useEffect(() => {
    function handleClickOutside(event) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsProfileOpen(false);
      }
    }
    function handleKeyDown(event) {
      if (event.key === "Escape") {
        setIsProfileOpen(false);
      }
    }
    if (isProfileOpen) {
      document.addEventListener("mousedown", handleClickOutside);
      document.addEventListener("keydown", handleKeyDown);
    }
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isProfileOpen]);

  const clinicianName = getClinicianName(currentUser);
  const clinicianRole = currentUser?.role || "DOCTOR";
  const clinicianEmail = currentUser?.email || "doctor1@careflow.ai";

  const handleNav = (path) => {
    setIsProfileOpen(false);
    navigate(path);
  };

  const handleLogout = async () => {
    setIsProfileOpen(false);
    await logout();
    navigate("/login");
  };

  return (
    <header className="sticky top-0 z-30 flex h-[72px] items-center justify-between border-b border-[var(--border-color)] bg-[var(--bg-primary)]/90 px-4 backdrop-blur-xl sm:px-6 lg:px-8">
      <div className="flex min-w-0 items-center gap-3">
        <button
          type="button"
          onClick={toggleMobileNav}
          className="rounded-lg p-2 text-[var(--text-secondary)] hover:bg-[var(--bg-hover)] lg:hidden"
          aria-label="Open navigation"
        >
          <Menu className="h-5 w-5" />
        </button>

        {/* Top-Left Clinician Pill (Interactive) */}
        <button
          type="button"
          onClick={() => setIsProfileOpen((prev) => !prev)}
          className="group hidden min-w-0 sm:flex sm:flex-col justify-center text-left rounded-xl px-2 py-1 -ml-1 transition hover:bg-[var(--bg-subtle)] focus:outline-none"
          title="Click to view clinician profile & quick navigation"
        >
          <div className="flex items-center gap-1.5">
            <span className="text-[10px] font-medium text-[var(--text-muted)] group-hover:text-teal-400 transition-colors">
              Apollo CareFlow Institute
            </span>
            <ChevronDown
              className={`h-3 w-3 text-[var(--text-muted)] group-hover:text-teal-400 transition-transform duration-200 ${
                isProfileOpen ? "rotate-180 text-teal-400" : ""
              }`}
            />
          </div>
          <div className="flex items-center gap-1.5">
            <span className="text-xs font-semibold text-[var(--text-primary)] group-hover:text-teal-300">
              {clinicianName}
            </span>
            <span className="rounded bg-teal-500/10 px-1.5 py-0.5 text-[9px] font-mono font-medium text-teal-400 border border-teal-500/20">
              {clinicianRole}
            </span>
          </div>
        </button>

        <div className="relative hidden w-[280px] md:block lg:ml-6">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--text-muted)]" />
          <input
            type="search"
            placeholder="Search patients, MRN…"
            className="h-10 w-full rounded-xl border border-[var(--border-color)] bg-[var(--bg-subtle)] pl-10 pr-4 text-xs text-[var(--text-primary)] outline-none transition focus:border-teal-500/50 focus:ring-2 focus:ring-teal-500/10"
          />
        </div>
      </div>

      <div className="flex items-center gap-1.5 sm:gap-2">
        {/* CareFlow Copilot Trigger */}
        <button
          type="button"
          onClick={toggleCopilot}
          className={[
            "flex items-center gap-2 rounded-lg px-2.5 py-1.5 text-xs font-medium transition-colors sm:px-3",
            isCopilotOpen
              ? "bg-teal-500 text-slate-950 font-semibold"
              : "border border-teal-500/20 bg-teal-500/5 text-teal-400 hover:bg-teal-500/10",
          ].join(" ")}
          title="Open CareFlow AI Copilot"
        >
          <Bot className="h-4 w-4" />
          <span className="hidden sm:inline">Copilot</span>
        </button>

        {/* Live Telemetry / STOMP Status */}
        <span
          className="inline-flex items-center gap-1.5 rounded-full bg-emerald-500/10 border border-emerald-500/20 px-2 py-0.5 text-[10px] font-medium text-emerald-400"
          title="Realtime STOMP-over-WebSocket active"
        >
          <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
          Live
        </span>

        {/* Notifications */}
        <button
          type="button"
          className="relative rounded-xl p-2.5 text-[var(--text-secondary)] transition hover:bg-[var(--bg-hover)] hover:text-[var(--text-primary)]"
          aria-label="Notifications"
        >
          <Bell className="h-[18px] w-[18px]" />
          {unreadNotifications > 0 && (
            <span className="absolute -right-0.5 -top-0.5 inline-flex h-4 min-w-4 items-center justify-center rounded-full bg-rose-500 px-1 text-[9px] font-bold leading-none text-white">
              {unreadNotifications > 99 ? "99+" : unreadNotifications}
            </span>
          )}
        </button>

        {/* Theme Toggle */}
        <button
          type="button"
          onClick={toggleTheme}
          className="rounded-xl p-2.5 text-[var(--text-secondary)] transition hover:bg-[var(--bg-hover)] hover:text-[var(--text-primary)]"
          aria-label="Toggle theme"
        >
          {theme === "dark" ? (
            <Sun className="h-[18px] w-[18px]" />
          ) : (
            <Moon className="h-[18px] w-[18px]" />
          )}
        </button>

        <div className="ml-1 hidden h-8 w-px bg-[var(--border-color)] sm:block" />

        {/* Top-Right Profile Avatar (Interactive) */}
        <div className="relative" ref={dropdownRef}>
          <button
            type="button"
            onClick={() => setIsProfileOpen((prev) => !prev)}
            className="flex items-center gap-2 rounded-full p-0.5 transition hover:ring-2 hover:ring-teal-500/30 focus:outline-none"
            aria-expanded={isProfileOpen}
            aria-haspopup="true"
            title="Clinician profile & actions"
          >
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-teal-500/15 text-xs font-semibold text-teal-400 border border-teal-500/30">
              {clinicianName.charAt(0) ?? "D"}
            </div>
          </button>

          {/* Profile Dropdown Menu */}
          {isProfileOpen && (
            <div className="absolute right-0 mt-3 w-80 sm:w-88 rounded-2xl border border-[var(--border-color)] bg-[var(--bg-sidebar)] p-4 shadow-2xl backdrop-blur-2xl z-50 animate-in fade-in zoom-in-95 duration-150">
              {/* Clinician Card */}
              <div className="flex items-start gap-3 pb-3 border-b border-[var(--border-color)]">
                <div className="relative flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-teal-500/20 text-teal-300 font-bold text-base border border-teal-500/30">
                  {clinicianName.charAt(0) ?? "D"}
                  <span className="absolute -bottom-0.5 -right-0.5 h-3 w-3 rounded-full bg-emerald-400 border-2 border-[var(--bg-sidebar)]" />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-1.5">
                    <p className="truncate text-sm font-semibold text-[var(--text-primary)]">
                      {clinicianName}
                    </p>
                    <span className="rounded bg-teal-500/15 px-1.5 py-0.2 text-[9px] font-mono font-medium text-teal-400 border border-teal-500/30">
                      {clinicianRole}
                    </span>
                  </div>
                  <p className="truncate text-xs text-[var(--text-muted)]">
                    {clinicianEmail}
                  </p>
                  <p className="mt-1 text-[11px] text-teal-400/90 font-medium flex items-center gap-1">
                    <Building2 className="h-3 w-3" />
                    Apollo CareFlow Institute · ICU
                  </p>
                </div>
              </div>

              {/* Staff ID & Status Badge */}
              <div className="my-3 flex items-center justify-between rounded-xl bg-[var(--bg-subtle)] px-3 py-2 text-[11px]">
                <div className="flex items-center gap-1.5 text-[var(--text-muted)]">
                  <ShieldCheck className="h-3.5 w-3.5 text-teal-400" />
                  <span>MCI License:</span>
                  <span className="font-mono font-semibold text-[var(--text-secondary)]">MCI-98421</span>
                </div>
                <span className="inline-flex items-center gap-1 text-[10px] font-medium text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full border border-emerald-500/20">
                  On Duty
                </span>
              </div>

              {/* Quick Clinical Navigation */}
              <div className="space-y-1 py-1">
                <p className="px-2 pb-1 text-[10px] font-semibold text-[var(--text-muted)] uppercase tracking-wider">
                  Quick Navigation
                </p>

                <button
                  type="button"
                  onClick={() => handleNav("/timeline")}
                  className="flex w-full items-center gap-2.5 rounded-xl px-2.5 py-2 text-left text-xs text-[var(--text-secondary)] hover:bg-[var(--bg-hover)] hover:text-[var(--text-primary)] transition"
                >
                  <Clock className="h-4 w-4 text-teal-400" />
                  <div>
                    <span className="font-medium">Clinical Timeline</span>
                    <p className="text-[10px] text-[var(--text-muted)]">Real-time audit log & patient events</p>
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => handleNav("/medications")}
                  className="flex w-full items-center gap-2.5 rounded-xl px-2.5 py-2 text-left text-xs text-[var(--text-secondary)] hover:bg-[var(--bg-hover)] hover:text-[var(--text-primary)] transition"
                >
                  <Pill className="h-4 w-4 text-emerald-400" />
                  <div>
                    <span className="font-medium">Medications & MAR</span>
                    <p className="text-[10px] text-[var(--text-muted)]">Active orders, administration records</p>
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => handleNav("/handoffs")}
                  className="flex w-full items-center gap-2.5 rounded-xl px-2.5 py-2 text-left text-xs text-[var(--text-secondary)] hover:bg-[var(--bg-hover)] hover:text-[var(--text-primary)] transition"
                >
                  <ArrowRightLeft className="h-4 w-4 text-sky-400" />
                  <div>
                    <span className="font-medium">Shift Handoffs</span>
                    <p className="text-[10px] text-[var(--text-muted)]">SBAR clinical handovers & continuity</p>
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => handleNav("/patients")}
                  className="flex w-full items-center gap-2.5 rounded-xl px-2.5 py-2 text-left text-xs text-[var(--text-secondary)] hover:bg-[var(--bg-hover)] hover:text-[var(--text-primary)] transition"
                >
                  <Users className="h-4 w-4 text-indigo-400" />
                  <div>
                    <span className="font-medium">Patients Census</span>
                    <p className="text-[10px] text-[var(--text-muted)]">Bed allocations & telemetry overview</p>
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => {
                    setIsProfileOpen(false);
                    toggleCopilot();
                  }}
                  className="flex w-full items-center gap-2.5 rounded-xl px-2.5 py-2 text-left text-xs text-[var(--text-secondary)] hover:bg-[var(--bg-hover)] hover:text-[var(--text-primary)] transition"
                >
                  <Sparkles className="h-4 w-4 text-amber-400" />
                  <div>
                    <span className="font-medium">CareFlow AI Copilot</span>
                    <p className="text-[10px] text-[var(--text-muted)]">Gemini clinical reasoning assistant</p>
                  </div>
                </button>
              </div>

              {/* Sign Out Button */}
              <div className="pt-2 mt-2 border-t border-[var(--border-color)]">
                <button
                  type="button"
                  onClick={handleLogout}
                  className="flex w-full items-center justify-center gap-2 rounded-xl bg-rose-500/10 px-3 py-2 text-xs font-semibold text-rose-400 hover:bg-rose-500/20 transition"
                >
                  <LogOut className="h-4 w-4" />
                  <span>Log out of session</span>
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Realtime Notification Toast */}
      {latestNotification && (
        <div className="fixed bottom-4 right-4 z-50 w-[calc(100vw-2rem)] max-w-[320px] rounded-2xl border border-[var(--border-color)] bg-[var(--bg-sidebar)] p-4 shadow-2xl">
          <div className="flex items-start gap-2.5">
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-rose-500/10 text-rose-400">
              <Bell className="h-4 w-4" />
            </div>
            <div className="min-w-0 flex-1">
              <p className="text-[11px] font-semibold text-[var(--text-primary)]">
                {latestNotification.type?.replaceAll("_", " ") ?? "Notification"}
              </p>
              <p className="mt-1 line-clamp-3 text-xs leading-5 text-[var(--text-secondary)]">
                {latestNotification.message}
              </p>
            </div>
            <button
              type="button"
              onClick={clearLatestNotification}
              className="rounded-lg p-1 text-[var(--text-muted)] hover:text-[var(--text-primary)]"
              aria-label="Dismiss notification"
            >
              ✕
            </button>
          </div>
        </div>
      )}
    </header>
  );
}
