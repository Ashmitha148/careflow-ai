import { Bot, FileSearch, Languages, MessageCircle, Sparkles, X } from "lucide-react";
import { useStore } from "../../store/useStore";
import { useAuth } from "../../context/AuthContext";
import {
  askTimelineQuestion,
  getPatientSummary,
  getPlainLanguage,
  getPatientPromptHistory,
} from "../../services/copilotApi";

const TABS = [
  { id: "qa", label: "Timeline Q&A", icon: MessageCircle },
  { id: "summary", label: "Summary", icon: Sparkles },
  { id: "plain", label: "Plain language", icon: Languages },
  { id: "explain", label: "Explainability", icon: FileSearch },
];

export default function CopilotSidePanel() {
  const { isCopilotOpen, toggleCopilot, selectedPatientId } = useStore();
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

  async function loadHistory() {
    if (!selectedPatientId) return;
    setLoadingHistory(true);
    try {
      const data = await getPatientPromptHistory(selectedPatientId);
      setHistoryList(data || []);
    } catch {
      // Graceful fallback
    } finally {
      setLoadingHistory(false);
    }
  }

  if (!isCopilotOpen) return null;

  function reset() {
    setError(null);
  }

  async function handleAsk(e) {
    e.preventDefault();
    if (!question.trim() || !selectedPatientId || !user?.id) return;
    reset();
    setLoading(true);
    try {
      const answer = await askTimelineQuestion(selectedPatientId, question);
      setQaAnswer(answer);
    } catch {
      setError("Copilot couldn't answer that just now.");
    } finally {
      setLoading(false);
    }
  }

  async function handleSummary() {
    if (!selectedPatientId || !user?.id) return;
    reset();
    setLoading(true);
    try {
      const text = await getPatientSummary(selectedPatientId);
      setSummary(text);
    } catch {
      setError("Copilot couldn't generate a summary just now.");
    } finally {
      setLoading(false);
    }
  }

  async function handlePlainLanguage(e) {
    e.preventDefault();
    if (!plainInput.trim() || !selectedPatientId || !user?.id) return;
    reset();
    setLoading(true);
    try {
      const text = await getPlainLanguage(selectedPatientId, plainInput);
      setPlainOutput(text);
    } catch {
      setError("Copilot couldn't translate that just now.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="fixed inset-y-0 right-0 z-50 flex w-full max-w-md flex-col border-l border-[var(--border-color)] bg-[var(--bg-sidebar)] shadow-2xl">
      <div className="flex items-center justify-between border-b border-[var(--border-color)] px-5 py-4">
        <div className="flex items-center gap-2">
          <Bot className="h-5 w-5 text-teal-400" />
          <h2 className="text-sm font-semibold">CareFlow Copilot</h2>
        </div>
        <button
          type="button"
          onClick={toggleCopilot}
          className="rounded-lg p-1.5 text-[var(--text-muted)] hover:bg-[var(--bg-hover)] hover:text-[var(--text-primary)]"
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      <div className="flex border-b border-[var(--border-color)] px-3 pt-3">
        {TABS.map(({ id, label, icon: Icon }) => (
          <button
            key={id}
            type="button"
            onClick={() => setTab(id)}
            className={`flex flex-1 items-center justify-center gap-1.5 rounded-t-lg px-2 py-2 text-[11px] font-medium transition ${
              tab === id
                ? "bg-[var(--bg-subtle)] text-teal-400"
                : "text-[var(--text-muted)] hover:text-[var(--text-secondary)]"
            }`}
          >
            <Icon className="h-3.5 w-3.5" />
            {label}
          </button>
        ))}
      </div>

      <div className="flex-1 overflow-y-auto px-5 py-4">
        {!selectedPatientId && (
          <p className="text-xs text-[var(--text-muted)]">Select a patient to use the Copilot.</p>
        )}

        {error && (
          <div className="mb-3 rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-xs text-rose-300">
            {error}
          </div>
        )}

        {selectedPatientId && tab === "qa" && (
          <div className="space-y-3">
            <form onSubmit={handleAsk} className="space-y-2">
              <textarea
                value={question}
                onChange={(e) => setQuestion(e.target.value)}
                placeholder="Ask about this patient's timeline..."
                rows={3}
                className="w-full rounded-lg border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3 py-2 text-xs outline-none focus:border-teal-500/50"
              />
              <button
                type="submit"
                disabled={loading}
                className="rounded-lg bg-teal-500 px-3 py-1.5 text-xs font-semibold text-slate-950 disabled:opacity-60"
              >
                {loading ? "Thinking..." : "Ask"}
              </button>
            </form>
            {qaAnswer && (
              <div className="space-y-1.5">
                <span className="inline-flex items-center gap-1 text-[10px] font-medium text-amber-400 bg-amber-400/10 px-2 py-0.5 rounded-full border border-amber-400/20">
                  AI-generated • Requires clinician review
                </span>
                <div className="rounded-xl border border-teal-500/20 bg-teal-500/5 px-4 py-3 text-xs leading-6 text-[var(--text-secondary)]">
                  {qaAnswer}
                </div>
              </div>
            )}
          </div>
        )}

        {selectedPatientId && tab === "summary" && (
          <div className="space-y-3">
            <button
              type="button"
              onClick={handleSummary}
              disabled={loading}
              className="inline-flex items-center gap-1.5 rounded-lg bg-teal-500 px-3 py-1.5 text-xs font-semibold text-slate-950 disabled:opacity-60"
            >
              <Sparkles className="h-3.5 w-3.5" />
              {loading ? "Summarizing..." : "Generate summary"}
            </button>
            {summary && (
              <div className="space-y-1.5">
                <span className="inline-flex items-center gap-1 text-[10px] font-medium text-amber-400 bg-amber-400/10 px-2 py-0.5 rounded-full border border-amber-400/20">
                  AI-generated • Requires clinician review
                </span>
                <div className="rounded-xl border border-teal-500/20 bg-teal-500/5 px-4 py-3 text-xs leading-6 text-[var(--text-secondary)] whitespace-pre-line">
                  {summary}
                </div>
              </div>
            )}
          </div>
        )}

        {selectedPatientId && tab === "plain" && (
          <div className="space-y-3">
            <form onSubmit={handlePlainLanguage} className="space-y-2">
              <textarea
                value={plainInput}
                onChange={(e) => setPlainInput(e.target.value)}
                placeholder="Paste clinical text to translate into plain language..."
                rows={3}
                className="w-full rounded-lg border border-[var(--border-color)] bg-[var(--bg-subtle)] px-3 py-2 text-xs outline-none focus:border-teal-500/50"
              />
              <button
                type="submit"
                disabled={loading}
                className="rounded-lg bg-teal-500 px-3 py-1.5 text-xs font-semibold text-slate-950 disabled:opacity-60"
              >
                {loading ? "Translating..." : "Translate"}
              </button>
            </form>
            {plainOutput && (
              <div className="space-y-1.5">
                <span className="inline-flex items-center gap-1 text-[10px] font-medium text-amber-400 bg-amber-400/10 px-2 py-0.5 rounded-full border border-amber-400/20">
                  AI-generated • Requires clinician review
                </span>
                <div className="rounded-xl border border-teal-500/20 bg-teal-500/5 px-4 py-3 text-xs leading-6 text-[var(--text-secondary)]">
                  {plainOutput}
                </div>
              </div>
            )}
          </div>
        )}

        {selectedPatientId && tab === "explain" && (
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <p className="text-xs font-medium text-[var(--text-primary)]">
                Audit Trail & Grounding Context
              </p>
              <button
                type="button"
                onClick={loadHistory}
                disabled={loadingHistory}
                className="text-[11px] text-teal-400 hover:underline disabled:opacity-60"
              >
                {loadingHistory ? "Loading..." : "Refresh"}
              </button>
            </div>

            <p className="text-[11px] text-[var(--text-muted)]">
              Inspect the exact timeline events retrieved and fed to Gemini as clinical context for each AI invocation.
            </p>

            {historyList.length === 0 && !loadingHistory && (
              <div className="rounded-xl border border-[var(--border-color)] bg-[var(--bg-subtle)] p-4 text-center text-xs text-[var(--text-muted)]">
                No AI prompts recorded for this patient yet.
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
                  <p className="text-[10px] font-medium text-[var(--text-muted)] uppercase tracking-wider">Grounding Context (Timeline Events Used):</p>
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
