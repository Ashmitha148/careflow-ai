import {
  Bell,
  Bot,
  Menu,
  Moon,
  Search,
  Sun,
} from "lucide-react";
import { useEffect } from "react";
import { useStore } from "../../store/useStore";
import { useAuth } from "../../context/AuthContext";
import { useUnreadCount } from "../../services/notificationsHooks";

const realtimeTone = {
  connected: "text-emerald-400",
  connecting: "text-amber-400",
  error: "text-rose-400",
  disconnected: "text-[var(--text-muted)]",
};

export default function Topbar() {
  const {
    toggleCopilot,
    isCopilotOpen,
    theme,
    toggleTheme,
    unreadNotifications,
    realtimeStatus,
    latestNotification,
    clearLatestNotification,
  } = useStore();
  const { user: currentUser } = useAuth();

  // Pull the authoritative unread count from the server (hydrates the store badge).
  useUnreadCount(currentUser?.id);

  // Auto-dismiss the realtime notification toast after a few seconds.
  useEffect(() => {
    if (!latestNotification) return undefined;
    const timer = setTimeout(clearLatestNotification, 6000);
    return () => clearTimeout(timer);
  }, [latestNotification, clearLatestNotification]);

  const connectionLabel =
    realtimeStatus === "connected"
      ? "Live"
      : realtimeStatus === "connecting"
        ? "Connecting…"
        : realtimeStatus === "error"
          ? "Realtime degraded"
          : "Offline";

  return (
    <header className="sticky top-0 z-30 flex h-[72px] items-center justify-between border-b border-[var(--border-color)] bg-[var(--bg-primary)]/90 px-4 backdrop-blur-xl sm:px-6 lg:px-8">
      <div className="flex min-w-0 items-center gap-3">
        <button
          type="button"
          className="rounded-lg p-2 text-[var(--text-secondary)] hover:bg-[var(--bg-hover)] lg:hidden"
          aria-label="Open navigation"
        >
          <Menu className="h-5 w-5" />
        </button>

        <div className="hidden min-w-0 sm:block">
          <p className="text-xs text-[var(--text-muted)]">Clinical workspace</p>
          <p className="truncate text-sm font-medium">
            Good to see you, {currentUser?.fullName?.split(" ")[0] ?? "there"}
          </p>
        </div>

        <div className="relative hidden w-[300px] md:block lg:ml-8">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--text-muted)]" />

          <input
            type="search"
            placeholder="Search patients or MRNs..."
            className="h-10 w-full rounded-xl border border-[var(--border-color)] bg-[var(--bg-subtle)] pl-10 pr-4 text-xs text-[var(--text-primary)] outline-none transition focus:border-teal-500/50 focus:ring-2 focus:ring-teal-500/10"
          />
        </div>
      </div>

      <div className="flex items-center gap-1.5 sm:gap-2">
        <button
          type="button"
          onClick={toggleCopilot}
          className={[
            "hidden items-center gap-2 rounded-xl px-3 py-2 text-xs font-medium transition-all sm:flex",
            isCopilotOpen
              ? "bg-teal-500 text-slate-950 shadow-lg shadow-teal-500/10"
              : "border border-teal-500/20 bg-teal-500/5 text-teal-400 hover:bg-teal-500/10",
          ].join(" ")}
        >
          <Bot className="h-4 w-4" />
          Copilot
        </button>

        <span
          className={`inline-flex items-center gap-1 rounded-full bg-[var(--bg-subtle)] px-2 py-1 text-[10px] font-medium ${realtimeTone[realtimeStatus] || realtimeTone.disconnected}`}
          title={`Realtime connection: ${realtimeStatus}`}
        >
          <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-current" />
          {connectionLabel}
        </span>

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

        <div className="hidden items-center gap-2 sm:flex">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-teal-500/10 text-xs font-semibold text-teal-400">
            {currentUser?.fullName?.charAt(0) ?? "C"}
          </div>
        </div>
      </div>

      {/* Real-time notification toast (fed by the socket bridge) */}
      {latestNotification && (
        <div className="fixed bottom-4 right-4 z-50 w-[320px] rounded-2xl border border-[var(--border-color)] bg-[var(--bg-sidebar)] p-4 shadow-2xl">
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
