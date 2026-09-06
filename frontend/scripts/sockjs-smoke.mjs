// Quick runtime smoke-test for the SockJS transport shim and the socket
// client's routing logic. Run: node scripts/sockjs-smoke.mjs
import { createSockJsWebSocket } from "../src/services/websocket/sockJsWebSocket.js";

function FakeWebSocket(url) {
  this.url = url;
  this.readyState = 0;
  this.onopen = null;
  this.onmessage = null;
  this.onclose = null;
  this.onerror = null;
  this.sent = [];
}
FakeWebSocket.prototype.send = function (data) {
  this.sent.push(typeof data === "string" ? data : new TextDecoder().decode(data));
};
FakeWebSocket.prototype.close = function () {
  this.readyState = 3;
  if (this.onclose) this.onclose({ code: 1000, reason: "" });
};

global.WebSocket = FakeWebSocket;

const sock = createSockJsWebSocket("http://localhost:8080/api/ws", "abc.def");

const seen = [];
sock.onopen = () => seen.push("open");
sock.onmessage = (evt) => seen.push(["msg", evt.data]);
sock.onclose = (evt) => seen.push(["close", evt.code]);

// Feed the session like a real SockJS server would.
const raw = sock._raw;
raw.onmessage({ data: JSON.stringify(["o"]) });
raw.onmessage({ data: JSON.stringify(["h"]) });
raw.onmessage({ data: JSON.stringify(['CONNECT\naccept-version:1.2\n\n\u0000']) });
raw.onmessage({ data: JSON.stringify(["c[3001]"]) });

// Wrap the "<frame>" content through the same stringify/decode the spec requires.
const wrapped = JSON.stringify(["x-message"]);
raw.onmessage({ data: wrapped });

sock.send("SEND\ndestination:/app/test\ncontent-length:2\n\n{}");

let fail = 0;
const assert = (cond, label) => {
  if (!cond) {
    fail += 1;
    console.error("FAIL:", label);
  } else {
    console.log("ok  :", label);
  }
};

assert(seen[0] === "open", "open frame fires onopen");
assert(
  seen.filter((item) => item[0] === "close").length === 1,
  "close frame cascades exactly one onclose",
);
assert(raw.sent.length === 1, "client sent exactly one datagram");
assert(JSON.parse(raw.sent[0])[0].startsWith("SEND\n"), "send wraps payload in a JSON array");
assert(String(sock._raw.url).includes("/000/"), "session URL has server+session segments");
assert(String(sock._raw.url).includes("token=abc.def"), "token is passed through the query string");

console.log("events:", JSON.stringify(seen));
console.log(fail === 0 ? "ALL PASS" : `${fail} FAILURES`);
process.exit(fail === 0 ? 0 : 1);