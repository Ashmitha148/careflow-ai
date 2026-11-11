import { useState } from "react";
import { Bot, FileSearch, FileText, Languages, MessageCircle, X, Sparkles, User, Activity } from "lucide-react";
import { useStore } from "../../store/useStore";
import { useAuth } from "../../context/AuthContext";
import {
  askTimelineQuestion,
  getPatientSummary,
  getPlainLanguage,
  getPatientPromptHistory,
} from "../../services/copilotApi";
import { MOCK_PATIENTS } from "../../services/mockDemoData";

const TABS = [
  { id: "qa", label: "Timeline Q&A", icon: MessageCircle },
  { id: "summary", label: "Summary", icon: FileText },
  { id: "plain", label: "Plain language", icon: Languages },
  { id: "explain", label: "Explainability", icon: FileSearch },
];

const QUICK_QUESTIONS = [
  "What are the latest vitals & telemetry status?",
  "Summarize medications administered today",
  "Are there any critical lab flags or alerts?",
];

export default function CopilotSidePanel() {
  const { isCopilotOpen, toggleCopilot, selectedPatientId, setSelectedPatientId } = useStore();
  const { user } = useAuth();
  const [tab, setTab] = useState("qa");

  const [question, setQuestion] = useState("");
  const [qaAnswer, setQaAnswer] = useState(null);

  const [summary, setSummary] = useState(null);
  const [plainInput, setPlainInput] = useState("");
  const [plainOutput, setPlainOutput] = useState(null);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const [historyList, setHistoryList] = useState([]);
  const [loadingHistory, setLoadingHistory] = useState(false);

  if (!isCopilotOpen) return null;

  // Resolve current active patient
  const activePatient =
    MOCK_PATIENTS.find((p) => p.id === selectedPatientId) || MOCK_PATIENTS[0];
  const patientId = activePatient?.id;

  async function loadHistory() {
    if (!patientId) return;
    setLoadingHistory(true);
    try {
      const data = await getPatientPromptHistory(patientId);
      setHistoryList(data || []);
    } catch {
      // Graceful fallback
    } finally {
      setLoadingHistory(false);
    }
  }

  function reset() {
    setError(null);
  }

  async function handleAskQuestion(queryText) {
    const q = queryText || question;
    if (!q.trim() || !patientId) return;
    reset();
    setLoading(true);
    try {
      const answer = await askTimelineQuestion(patientId, q);
      setQaAnswer(answer);
    } catch {
      setError("Copilot couldn't answer that just now. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  async function handleAsk(e) {
    e.preventDefault();
    handleAskQuestion(question);
  }

  async function handleSummary() {
    if (!patientId) return;
    reset();
    setLoading(true);
    try {
      const text = await getPatientSummary(patientId);
      setSummary(text);
    } catch {
      setError("Copilot couldn't generate a summary just now.");
    } finally {
      setLoading(false);
    }
  }

  async function handlePlainLanguage(e) {
    e.preventDefault();
    if (!plainInput.trim() || !patientId) return;
    reset();
    setLoading(true);
    try {
      const text = await getPlainLanguage(patientId, plainInput);
      setPlainOutput(text);
    } catch {
      setError("Copilot couldn't translate that just now.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="fixed inset-y-0 right-0 z-50 flex w-full max-w-md flex-col border-l border-[var(--border-color)] bg-[var(--bg-sidebar)] shadow-2xl backdrop-blur-xl">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-[var(--border-color)] px-5 py-4">
        <div className="flex items-center gap-2.5">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-teal-500/10 text-teal-400 border border-teal-500/20">
            <Bot className="h-5 w-5" />
          </div>
          <div>
            <h2 className="text-sm font-semibold text-[var(--text-primary)]">CareFlow Copilot</h2>
            <p className="text-[10px] text-teal-400 font-medium">Gemini 2.5 Clinical Reasoning Engine</p>
          </div>
        </div>
        <button
          type="button"
          onClick={toggleCopilot}
          className="rounded-lg p-1.5 text-[var(--text-muted)] hover:bg-[var(--bg-hover)] hover:text-[var(--text-primary)] transition"
          aria-label="Close Copilot"
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      {/* Patient Selector */}
      <div className="flex items-center justify-between border-b border-[var(--border-color)] bg-[var(--bg-subtle)] px-4 py-2.5">
        <div className="flex items-center gap-2">
          <User className="h-3.5 w-3.5 text-teal-400" />
          <span className="text-[11px] font-medium text-[var(--text-secondary)]">Active Inpatient:</span>
        </div>
        <select
          value={patientId}
          onChange={(e) => {
            setSelectedPatientId(e.target.value);
            setQaAnswer(null);
            setSummary(null);
            setPlainOutput(null);
          }}
          className="rounded-lg border border-[var(--border-color)] bg-[var(--bg-primary)] px-2.5 py-1 text-xs font-semibold text-teal-400 outline-none focus:border-teal-500 cursor-pointer"
        >
          {MOCK_PATIENTS.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name} ({p.mrn} · {p.room})
            </option>
          ))}
        </select>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-[var(--border-color)] px-3 pt-3">
        {TABS.map(({ id, label, icon: Icon }) => (
          <button
            key={id}
            type="button"
            onClick={() => setTab(id)}
            className={`flex flex-1 items-center justify-center gap-1.5 rounded-t-lg px-2 py-2 text-[11px] font-medium transition ${
              tab === id
                ? "bg-[var(--bg-subtle)] text-teal-400 border-b-2 border-teal-400"
                : "text-[var(--text-muted)] hover:text-[var(--text-secondary)]"
            }`}
          >
            <Icon className="h-3.5 w-3.5" />
            <span className="hidden sm:inline">{label}</span>
          </button>
        ))}
      </div>

      {/* Tab Body */}
      <div className="flex-1 overflow-y-auto px-5 py-4 space-y-4">
        {error && (
          <div className="rounded-xl border border-rose-500/30 bg-rose-500/10 px-3.5 py-2.5 text-xs text-rose-300">
            {error}
          </div>
        )}

        {/* Tab: Timeline Q&A */}
        {tab === "qa" && (
          <div className="space-y-4">
            <div>
              <p className="text-[11px] font-medium text-[var(--text-muted)] mb-2">Suggested queries:</p>
              <div className="flex flex-col gap-1.5">
                {QUICK_QUESTIONS.map((chip, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => {
                      setQuestion(chip);
                      handleAskQuestion(chip);
                    }}
                    className="text-left rounded-lg border border-[var(--border-color)] bg-[var(--bg-subtle)] px-2.5 py-1.5 text-[11px] text-[var(--text-secondary)] hover:border-teal-500/40 hover:text-teal-300 transition"
                  >
                    ✨ {chip}
                  </button>
                ))}
              </div>
            </div>

            <form onSubmit={handleAsk} className="space-y-2">
              <textarea
                value={question}
                onChange={(e) => setQuestion(e.target.value)}
                placeholder={`Ask Copilot about ${activePatient.name}'s telemetry, labs, or timeline...`}
                rows={3}
                className="w-full rounded-xl border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3 py-2.5 text-xs outline-none transition focus:border-teal-500/50 focus:ring-2 focus:ring-teal-500/10"
              />
              <button
                type="submit"
                disabled={loading || !question.trim()}
                className="inline-flex items-center gap-2 rounded-xl bg-teal-500 px-4 py-2 text-xs font-semibold text-slate-950 hover:bg-teal-400 disabled:opacity-50 transition"
              >
                <Sparkles className="h-3.5 w-3.5" />
                {loading ? "Analyzing Clinical Context..." : "Ask Copilot"}
              </button>
            </form>

            {qaAnswer && (
              <div className="space-y-2 pt-2">
                <span className="inline-flex items-center gap-1.5 text-[10px] font-medium text-amber-400 bg-amber-400/10 px-2 py-0.5 rounded-full border border-amber-400/20">
                  <Activity className="h-3 w-3" />
                  AI-Grounded • Verified with Patient Timeline
                </span>
                <div className="rounded-xl border border-teal-500/20 bg-teal-500/5 p-4 text-xs leading-relaxed text-[var(--text-primary)] shadow-sm">
                  {qaAnswer}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Tab: Patient Summary */}
        {tab === "summary" && (
          <div className="space-y-4">
            <p className="text-xs text-[var(--text-secondary)]">
              Synthesize a structured SBAR executive handover report for <span className="font-semibold text-teal-400">{activePatient.name}</span> based on inpatient telemetry, notes, and medication administration.
            </p>
            <button
              type="button"
              onClick={handleSummary}
              disabled={loading}
              className="inline-flex items-center gap-2 rounded-xl bg-teal-500 px-4 py-2 text-xs font-semibold text-slate-950 hover:bg-teal-400 disabled:opacity-50 transition"
            >
              <FileText className="h-3.5 w-3.5" />
              {loading ? "Generating SBAR Summary..." : "Generate SBAR Summary"}
            </button>
            {summary && (
              <div className="space-y-2 pt-2">
                <span className="inline-flex items-center gap-1.5 text-[10px] font-medium text-amber-400 bg-amber-400/10 px-2 py-0.5 rounded-full border border-amber-400/20">
                  <Activity className="h-3 w-3" />
                  Audit-grounded • Clinician signoff required
                </span>
                <div className="rounded-xl border border-teal-500/20 bg-teal-500/5 p-4 text-xs leading-relaxed text-[var(--text-primary)] whitespace-pre-line shadow-sm">
                  {summary}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Tab: Plain Language */}
        {tab === "plain" && (
          <div className="space-y-4">
            <p className="text-xs text-[var(--text-secondary)]">
              Translate complex clinical jargon, physician notes, or discharge orders into compassionate, plain language for {activePatient.name} and their family.
            </p>
            <form onSubmit={handlePlainLanguage} className="space-y-2">
              <textarea
                value={plainInput}
                onChange={(e) => setPlainInput(e.target.value)}
                placeholder="Paste clinical note, diagnosis, or lab results..."
                rows={3}
                className="w-full rounded-xl border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3 py-2.5 text-xs outline-none transition focus:border-teal-500/50 focus:ring-2 focus:ring-teal-500/10"
              />
              <button
                type="submit"
                disabled={loading || !plainInput.trim()}
                className="inline-flex items-center gap-2 rounded-xl bg-teal-500 px-4 py-2 text-xs font-semibold text-slate-950 hover:bg-teal-400 disabled:opacity-50 transition"
              >
                <Languages className="h-3.5 w-3.5" />
                {loading ? "Translating..." : "Translate to Plain Language"}
              </button>
            </form>
            {plainOutput && (
              <div className="space-y-2 pt-2">
                <span className="inline-flex items-center gap-1 text-[10px] font-medium text-emerald-400 bg-emerald-400/10 px-2 py-0.5 rounded-full border border-emerald-400/20">
                  Family & Patient Accessible
                </span>
                <div className="rounded-xl border border-teal-500/20 bg-teal-500/5 p-4 text-xs leading-relaxed text-[var(--text-primary)] whitespace-pre-line shadow-sm">
                  {plainOutput}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Tab: Explainability & Audit Trail */}
        {tab === "explain" && (
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <p className="text-xs font-medium text-[var(--text-primary)]">
                Audit Trail & Grounding Context
              </p>
              <button
                type="button"
                onClick={loadHistory}
                disabled={loadingHistory}
                className="text-[11px] text-teal-400 hover:underline disabled:opacity-60 font-semibold"
              >
                {loadingHistory ? "Loading..." : "Refresh Trail"}
              </button>
            </div>

            <p className="text-[11px] text-[var(--text-muted)]">
              Inspect the exact timeline events retrieved and fed to Gemini as clinical context for each AI invocation.
            </p>

            {historyList.length === 0 && !loadingHistory && (
              <div className="rounded-xl border border-[var(--border-color)] bg-[var(--bg-subtle)] p-4 text-center text-xs text-[var(--text-muted)]">
                Click "Refresh Trail" or run a query above to view audit grounding records.
              </div>
            )}

            {historyList.map((item) => (
              <div key={item.id} className="rounded-xl border border-[var(--border-color)] bg-[var(--bg-subtle)] p-3 space-y-2">
                <div className="flex items-center justify-between text-[11px]">
                  <span className="font-semibold text-teal-400">{item.featureName}</span>
                  <span className="text-[10px] text-[var(--text-muted)]">
                    {new Date(item.createdAt).toLocaleTimeString()}
                  </span>
                </div>
                
                <div>
                  <p className="text-[10px] font-medium text-[var(--text-muted)] uppercase tracking-wider">Grounding Context:</p>
                  <pre className="mt-1 max-h-36 overflow-y-auto rounded-lg bg-black/40 p-2 font-mono text-[10px] text-slate-300 whitespace-pre-wrap leading-tight">
                    {item.retrievedContext || "No timeline context attached"}
                  </pre>
                </div>

                <div>
                  <p className="text-[10px] font-medium text-[var(--text-muted)] uppercase tracking-wider">AI Generated Output:</p>
                  <p className="mt-0.5 text-[11px] text-[var(--text-secondary)] leading-relaxed line-clamp-3">
                    {item.aiResponse}
                  </p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
