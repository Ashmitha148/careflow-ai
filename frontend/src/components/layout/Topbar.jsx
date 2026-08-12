import {
  Bell,
  Bot,
  Menu,
  Moon,
  Search,
  Sun,
} from "lucide-react";
import { useStore } from "../../store/useStore";
import { useAuth } from "../../context/AuthContext";

export default function Topbar() {
  const {
    toggleCopilot,
    isCopilotOpen,
    theme,
    toggleTheme,
    unreadNotifications,
  } = useStore();
  const { user: currentUser } = useAuth();

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

        <button
          type="button"
          className="relative rounded-xl p-2.5 text-[var(--text-secondary)] transition hover:bg-[var(--bg-hover)] hover:text-[var(--text-primary)]"
          aria-label="Notifications"
        >
          <Bell className="h-[18px] w-[18px]" />

          {unreadNotifications > 0 && (
            <span className="absolute right-2 top-2 h-1.5 w-1.5 rounded-full bg-rose-400 ring-2 ring-[var(--bg-primary)]" />
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
    </header>
  );
}
