// LIVE end-to-end smoke test against a running backend (default :8080).
// Verifies the Phase 1 WebSocket layer: JWT-gated SockJS handshake, STOMP
// CONNECT, authorized subscriptions, and real-time MESSAGE delivery —
// a clinical event pushed over REST must arrive on the socket topic.
// Run: node scripts/ws-live-smoke.mjs [baseUrl]
import { createSockJsWebSocket } from "../src/services/websocket/sockJsWebSocket.js";

const BASE = process.argv[2] || "http://localhost:8080";
const PATIENT_ID = "a1111111-1111-1111-1111-111111111111";
const DOCTOR_ID = "11111111-1111-1111-1111-111111111111";

let failures = 0;
const assert = (cond, label) => {
  if (!cond) { failures += 1; console.error("FAIL:", label); }
  else console.log("ok  :", label);
};

/** Minimal STOMP-over-SockJS session built on the production shim. */
function stompSession(token) {
  const messages = [];
  const waiters = [];
  let cursor = 0; // next() consumes messages monotonically from this index
  let closed = false;

  const socket = createSockJsWebSocket(`${BASE}/api/ws`, token);
  socket.onmessage = (evt) => {
    messages.push(String(evt.data));
    for (const w of [...waiters]) w();
  };
  const closedPromise = new Promise((resolve) => {
    socket.onclose = () => { closed = true; resolve(); };
    socket.onerror = () => { /* close event follows */ };
  });

  const send = (text) => socket.send(text);

  return {
    socket,
    closed: closedPromise,
    isClosed: () => closed,
    connect: () =>
      send("CONNECT\naccept-version:1.2\nheart-beat:10000,10000\n\n\u0000"),
    subscribe: (destination, id) =>
      send(`SUBSCRIBE\ndestination:${destination}\nid:${id}\n\n\u0000`),
    disconnect: (receipt) =>
      send(`DISCONNECT\nreceipt:${receipt}\n\n\u0000`),
    messages: () => messages,
    /** Resolves with the next unconsumed message starting with `prefix`.
     *  Uses a monotonic cursor so frames arriving between the trigger action
     *  (e.g. the REST POST) and this call are not missed. */
    async next(prefix, timeoutMs = 6000) {
      const check = () => {
        for (let i = cursor; i < messages.length; i++) {
          if (messages[i].startsWith(prefix)) {
            cursor = i + 1;
            return messages[i];
          }
        }
        return closed ? null : undefined;
      };
      const immediate = check();
      if (immediate !== undefined) return immediate;
      return new Promise((resolve) => {
        const timer = setTimeout(() => {
          const i = waiters.indexOf(wake);
          if (i !== -1) waiters.splice(i, 1);
          resolve(null);
        }, timeoutMs);
        const wake = () => {
          const hit = check();
          if (hit !== undefined) { clearTimeout(timer); resolve(hit); }
        };
        waiters.push(wake);
      });
    },
  };
}

async function main() {
  // 0. Login as the seeded demo doctor.
  const res = await fetch(`${BASE}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: "doctor1@careflow.ai", password: "password123" }),
  });
  assert(res.ok, "demo doctor login succeeds over HTTP");
  const { token } = await res.json();
  assert(Boolean(token), "login response carries an access token");

  // 1. Unauthenticated upgrade must be rejected by the handshake interceptor.
  {
    const s = stompSession("");
    const outcome = await Promise.race([
      s.closed.then(() => "closed"),
      new Promise((r) => setTimeout(() => r("timeout"), 6000)),
    ]);
    assert(outcome === "closed", "unauthenticated upgrade is rejected (connection closed)");
  }

  // 2. Authenticated handshake + STOMP CONNECT + CONNECTED.
  const s = stompSession(token);
  s.connect();
  const connected = await s.next("CONNECTED");
  assert(Boolean(connected), "valid JWT completes the handshake and STOMP CONNECT");

  // 3. Authorized subscription registers with the broker.
  s.subscribe(`/topic/patient/${PATIENT_ID}/vitals`, "vitals");

  // 4. THE definitive test: a clinical event pushed via REST must arrive as a
  //    MESSAGE frame on the subscribed topic — real-time push, not refetch.
  const value = `94.${Date.now() % 10}`;
  const postRes = await fetch(`${BASE}/api/clinical/vitals/${PATIENT_ID}`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: JSON.stringify({ type: "OXYGEN", value }),
  });
  assert(postRes.ok, `POST /api/vitals succeeds (status ${postRes.status})`);

  const message = await s.next("MESSAGE");
  assert(
    Boolean(message) &&
      message.includes(`destination:/topic/patient/${PATIENT_ID}/vitals`) &&
      message.includes(value),
    "recorded vital arrives live on /topic/patient/{id}/vitals",
  );

  // 5. Server-side cleanup: DISCONNECT is honored with a RECEIPT.
  s.disconnect("r-bye");
  const receipt = await s.next("RECEIPT", 4000);
  assert(Boolean(receipt) && receipt.includes("receipt-id:r-bye"),
    "DISCONNECT produces the requested RECEIPT");

  try { s.socket.close(); } catch { /* already closed */ }

  console.log(failures === 0 ? "ALL PASS" : "FAILURES PRESENT");
  process.exit(failures === 0 ? 0 : 1);
}

main().catch((err) => {
  console.error("SMOKE ERROR:", err.message);
  process.exit(1);
});
