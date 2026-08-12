import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { Activity } from "lucide-react";
import { useAuth } from "../context/AuthContext";

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

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

  return (
    <div className="flex min-h-screen items-center justify-center bg-[var(--bg-primary)] px-4">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex flex-col items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-teal-500 text-slate-950 shadow-lg shadow-teal-500/20">
            <Activity className="h-6 w-6" strokeWidth={2.2} />
          </div>
          <div className="text-center">
            <p className="text-lg font-semibold text-[var(--text-primary)]">CareFlow AI</p>
            <p className="text-xs text-[var(--text-muted)]">Sign in to continue</p>
          </div>
        </div>

        <form
          onSubmit={handleSubmit}
          className="space-y-4 rounded-2xl border border-[var(--border-color)] bg-[var(--bg-sidebar)] p-6"
        >
          {error && (
            <div className="rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-xs text-rose-300">
              {error}
            </div>
          )}

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
        </form>
      </div>
    </div>
  );
}
