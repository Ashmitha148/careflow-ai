// Runtime smoke-test for RealtimeSocketClient routing + offline buffering.
// Run: node scripts/socketclient-smoke.mjs
import { RealtimeSocketClient } from "../src/services/websocket/socketClient.js";

const client = new RealtimeSocketClient({
  getToken: () => "jwt-token",
  getUrl: () => "http://localhost:8080/api/ws",
  onStatusChange: () => {},
});

let delivered = [];
const handler = (payload, frame) => delivered.push({ payload, frame });

let exitCode = 0;
const assert = (cond, label) => {
  if (!cond) {
    exitCode = 1;
    console.error("FAIL:", label);
  } else {
    console.log("ok  :", label);
  }
};

// 1. Subscribing while "offline" must register the destination (re-issued on
//    connect), not throw.
client.subscribe("/topic/patient/p1/timeline", handler);
assert(
  client.desired.has("/topic/patient/p1/timeline"),
  "offline subscribe is buffered",
);

// 2. Sending presence control while offline buffers it for the next connect.
client.send("/app/presence/patient/p1", { action: "JOIN" });
assert(client.pendingAppSends.length === 1, "offline app-send is buffered");

// 3. Routing fans out to handlers and parses JSON (route is invoked by the
//    STOMP callback with the raw frame).
client.route("/topic/patient/p1/timeline", {
  body: JSON.stringify({ id: "e1", patientId: "p1", eventType: "VITAL" }),
});
assert(
  delivered.length === 1 && delivered[0].payload.id === "e1",
  "route() delivers parsed JSON payload",
);

// 4. Connected send goes straight to forwardSend (which mints the STOMP SEND).
let published = [];
client.connected = true;
client.forwardSend = (destination, body) => {
  published.push({ destination, body });
  return true;
};
client.send("/app/presence/patient/p1", { action: "LEAVE" });
assert(
  published.length === 1 && published[0].destination === "/app/presence/patient/p1",
  "connected send forwards immediately",
);

// 5. Unsubscribe of the last handler removes the destination.
client.unsubscribe("/topic/patient/p1/timeline", handler);
assert(
  !client.desired.has("/topic/patient/p1/timeline"),
  "unsubscribe of last handler drops the destination",
);

// 6. Non-JSON body reaches the handler as a string, not a crash.
//    (Re-subscribe — the handler was removed by the unsubscribe test above.)
client.subscribe("/topic/patient/p1/timeline", handler);
client.route("/topic/patient/p1/timeline", { body: "plain-text" });
assert(delivered[1]?.payload === "plain-text", "non-JSON body passes through");

// 7. NEW subscriptions while connected call issueSubscription (STOMP SUBSCRIBE).
let issued = [];
client.issueSubscription = (destination) => issued.push(destination);
client.connected = true;
client.subscribe("/user/queue/notifications", () => {});
assert(
  issued.includes("/user/queue/notifications"),
  "connected subscribe issues a STOMP subscription",
);

console.log(exitCode === 0 ? "ALL PASS" : "FAILURES PRESENT");
process.exit(exitCode);