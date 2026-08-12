import { useEffect, useState } from "react";
import { ShieldCheck } from "lucide-react";
import { getAuditLogs } from "../../services/auditApi";

function formatDate(value) {
  if (!value) return "Unknown time";
  return new Date(value).toLocaleString([], { dateStyle: "medium", timeStyle: "short" });
}

export default function AuditPanel() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    getAuditLogs(0, 20)
      .then((data) => setLogs(data?.content || []))
      .catch((err) => setError(err))
      .finally(() => setLoading(false));
  }, []);

  return (
    <section className="surface overflow-hidden rounded-2xl">
      <div className="flex items-center gap-2 border-b border-[var(--border-color)] px-5 py-4">
        <ShieldCheck className="h-4 w-4 text-teal-400" />
        <h2 className="text-sm font-semibold">Audit trail</h2>
      </div>

      {loading && (
        <div className="px-5 py-8 text-center text-sm text-[var(--text-muted)]">Loading audit log...</div>
      )}

      {error && !loading && (
        <div className="px-5 py-8 text-center text-sm text-rose-400">Unable to load audit log.</div>
      )}

      {!loading && !error && logs.length === 0 && (
        <div className="px-5 py-8 text-center text-sm text-[var(--text-muted)]">No audit activity yet.</div>
      )}

      {!loading && !error && logs.length > 0 && (
        <div className="divide-y divide-[var(--border-color)]">
          {logs.map((log) => (
            <div key={log.id} className="flex items-center justify-between gap-3 px-5 py-3">
              <div className="min-w-0">
                <p className="truncate text-xs font-medium">
                  {log.action} &middot; {log.entityType}
                </p>
                <p className="mt-0.5 truncate text-[11px] text-[var(--text-muted)]">
                  {log.user?.fullName || "System"}
                </p>
              </div>
              <span className="shrink-0 text-[10px] text-[var(--text-muted)]">
                {formatDate(log.timestamp)}
              </span>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
