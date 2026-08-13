import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { Activity, Stethoscope, Heart, Users, User, ChevronLeft, Zap } from "lucide-react";
import { useAuth } from "../context/AuthContext";

// ─── Demo role definitions (only seeded accounts from V2 + V5 migrations) ───
const DEMO_ROLES = [
  {
    key: "DOCTOR",
    label: "Doctor",
    email: "doctor1@careflow.ai",
    password: "password123",
    icon: Stethoscope,
    color: "teal",
    desc: "View patient workspaces, write diagnoses, prescribe medications, manage clinical tasks, and see AI-generated summaries.",
  },
  {
    key: "NURSE",
    label: "Nurse",
    email: "nurse1@careflow.ai",
    password: "password123",
    icon: Heart,
    color: "sky",
    desc: "Manage your shift queue, record vitals, administer medications, and generate AI shift handoff reports.",
  },
  {
    key: "CAREGIVER",
    label: "Caregiver",
    email: "caregiver@careflow.ai",
    password: "password123",
    icon: Users,
    color: "violet",
    desc: "Monitor your family member's care plan, view timeline events, medication schedules, and upcoming appointments.",
  },
  {
    key: "PATIENT",
    label: "Patient",
    email: "patient@careflow.ai",
    password: "password123",
    icon: User,
    color: "amber",
    desc: "See your own care dashboard, upcoming medications, scheduled appointments, and submit a video verification.",
  },
];

const colorMap = {
  teal:   { ring: "ring-teal-500/40",   bg: "bg-teal-500/10",   text: "text-teal-400",   btn: "bg-teal-500 hover:bg-teal-400"   },
  sky:    { ring: "ring-sky-500/40",    bg: "bg-sky-500/10",    text: "text-sky-400",    btn: "bg-sky-500 hover:bg-sky-400"    },
  violet: { ring: "ring-violet-500/40", bg: "bg-violet-500/10", text: "text-violet-400", btn: "bg-violet-500 hover:bg-violet-400" },
  amber:  { ring: "ring-amber-500/40",  bg: "bg-amber-500/10",  text: "text-amber-400",  btn: "bg-amber-500 hover:bg-amber-400"  },
};

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  // ── Normal login state ──
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  // ── Demo mode state ──
  const [showDemo, setShowDemo] = useState(false);
  const [demoLoading, setDemoLoading] = useState(null); // key of loading role

  const from = location.state?.from?.pathname || "/";

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      await login(email, password);
      navigate(from, { replace: true });
    } catch (err) {
      setError(
        err?.response?.data?.message || "Invalid email or password. Please try again."
      );
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDemoLogin(role) {
    setError("");
    setDemoLoading(role.key);
    try {
      // Uses the real /api/auth/login endpoint — no credential exposure to UI
      await login(role.email, role.password);
      navigate(from, { replace: true });
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          `Could not log in as demo ${role.label}. Make sure the backend is running.`
      );
      setShowDemo(false);
    } finally {
      setDemoLoading(null);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-[var(--bg-primary)] px-4">
      <div className="w-full max-w-sm">
        {/* ── Logo / Header ── */}
        <div className="mb-8 flex flex-col items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-teal-500 text-slate-950 shadow-lg shadow-teal-500/20">
            <Activity className="h-6 w-6" strokeWidth={2.2} />
          </div>
          <div className="text-center">
            <p className="text-lg font-semibold text-[var(--text-primary)]">
              CareFlow AI
            </p>
            <p className="text-xs text-[var(--text-muted)]">
              {showDemo ? "Choose a role to explore" : "Sign in to continue"}
            </p>
          </div>
        </div>

        {/* ── Error Banner (shared) ── */}
        {error && (
          <div className="mb-3 rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-xs text-rose-300">
            {error}
          </div>
        )}

        {/* ══════════════════════════════════
            DEMO ROLE PICKER
        ══════════════════════════════════ */}
        {showDemo ? (
          <div className="space-y-3 rounded-2xl border border-[var(--border-color)] bg-[var(--bg-sidebar)] p-5">
            <button
              onClick={() => { setShowDemo(false); setError(""); }}
              className="mb-1 flex items-center gap-1 text-xs text-[var(--text-muted)] transition hover:text-[var(--text-primary)]"
            >
              <ChevronLeft className="h-3.5 w-3.5" />
              Back to sign in
            </button>

            <p className="text-xs font-medium text-[var(--text-secondary)]">
              Select a role — we'll log you in instantly
            </p>

            {DEMO_ROLES.map((role) => {
              const Icon = role.icon;
              const c = colorMap[role.color];
              const isLoading = demoLoading === role.key;
              return (
                <button
                  key={role.key}
                  onClick={() => handleDemoLogin(role)}
                  disabled={demoLoading !== null}
                  className={`group w-full rounded-xl border bg-[var(--bg-subtle)] p-3.5 text-left transition-all duration-200
                    hover:bg-[var(--bg-hover)] disabled:cursor-not-allowed disabled:opacity-50
                    ${isLoading ? `ring-2 ${c.ring}` : "border-[var(--border-color)] hover:border-[var(--text-muted)]/30"}`}
                >
                  <div className="flex items-start gap-3">
                    <div className={`mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg ${c.bg}`}>
                      <Icon className={`h-4 w-4 ${c.text}`} />
                    </div>
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center justify-between">
                        <span className={`text-sm font-semibold ${isLoading ? c.text : "text-[var(--text-primary)]"}`}>
                          {role.label}
                        </span>
                        {isLoading && (
                          <span className={`text-xs font-medium ${c.text}`}>
                            Signing in…
                          </span>
                        )}
                      </div>
                      <p className="mt-0.5 text-xs leading-relaxed text-[var(--text-muted)]">
                        {role.desc}
                      </p>
                    </div>
                  </div>
                </button>
              );
            })}

            <p className="pt-1 text-center text-[10px] text-[var(--text-muted)]">
              Demo accounts use read-only seeded data. No real patient info.
            </p>
          </div>
        ) : (
          /* ══════════════════════════════════
              NORMAL LOGIN FORM
          ══════════════════════════════════ */
          <form
            onSubmit={handleSubmit}
            className="space-y-4 rounded-2xl border border-[var(--border-color)] bg-[var(--bg-sidebar)] p-6"
          >
            <div>
              <label className="mb-1 block text-xs font-medium text-[var(--text-secondary)]">
                Email
              </label>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@careflow.ai"
                className="w-full rounded-lg border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3 py-2 text-sm text-[var(--text-primary)] outline-none focus:border-teal-500/50 focus:ring-2 focus:ring-teal-500/10"
              />
            </div>

            <div>
              <label className="mb-1 block text-xs font-medium text-[var(--text-secondary)]">
                Password
              </label>
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="********"
                className="w-full rounded-lg border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3 py-2 text-sm text-[var(--text-primary)] outline-none focus:border-teal-500/50 focus:ring-2 focus:ring-teal-500/10"
              />
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="w-full rounded-lg bg-teal-500 py-2 text-sm font-semibold text-slate-950 transition hover:bg-teal-400 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {submitting ? "Signing in..." : "Sign in"}
            </button>

            {/* ── Demo CTA ── */}
            <div className="relative flex items-center gap-3">
              <div className="h-px flex-1 bg-[var(--border-color)]" />
              <span className="text-[10px] text-[var(--text-muted)]">or</span>
              <div className="h-px flex-1 bg-[var(--border-color)]" />
            </div>

            <button
              type="button"
              onClick={() => { setShowDemo(true); setError(""); }}
              className="flex w-full items-center justify-center gap-2 rounded-lg border border-[var(--border-color)] bg-[var(--bg-subtle)] py-2 text-sm font-medium text-[var(--text-secondary)] transition hover:border-teal-500/40 hover:bg-[var(--bg-hover)] hover:text-[var(--text-primary)]"
            >
              <Zap className="h-4 w-4 text-teal-400" />
              Enter Demo
            </button>

            <p className="text-center text-xs text-[var(--text-muted)]">
              Need an account?{" "}
              <Link to="/register" className="text-teal-400 hover:text-teal-300">
                Create one
              </Link>
            </p>
          </form>
        )}
      </div>
    </div>
  );
}
