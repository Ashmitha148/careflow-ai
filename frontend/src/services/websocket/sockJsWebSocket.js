/**
 * Minimal SockJS 1.x "websocket transport" client.
 *
 * SockJS is used as a fallback transport for STOMP when the environment blocks
 * the raw WebSocket upgrade (ancient proxies, some PaaS edge layers). The
 * backend exposes it on the same `/ws/**` endpoint via StompEndpointRegistry
 * `.withSockJS()`. The server registers both transports, so we simply pick the
 * one that connects.
 *
 * SockJS protocol (dying topics of the spec):
 *   - client connects to /ws/{serverId}/{sessionId}/websocket?token=...
 *     where serverId = "000" and sessionId is a random digit string.
 *   - server -> client frames are JSON arrays of strings:
 *       ["o"]                 open
 *       ["h"]                 heartbeat
 *       ["c<code>"]           close
 *       ["<payload>"]         application message
 *   - client -> server frames are JSON arrays of strings, one per message.
 *
 * This object implements the same duck-typed WebSocket contract the
 * @stomp/stompjs StompHandler consumes (onopen/onmessage/onclose/onerror,
 * send, close, terminate, readyState, url, binaryType).
 */
const CONNECTING = 0;
const OPEN = 1;
const CLOSING = 2;
const CLOSED = 3;

function randomSessionId() {
  let id = "";
  for (let i = 0; i < 12; i += 1) id += Math.floor(Math.random() * 10);
  return id;
}

export function createSockJsWebSocket(baseUrl, token) {
  const normalized = String(baseUrl).replace(/\/+$/, "");
  const sessionUrl =
    `${normalized}/000/${randomSessionId()}/websocket?token=${encodeURIComponent(token || "")}`;

  const raw = new WebSocket(sessionUrl);
  const socket = new SockJsWebSocket(raw);

  raw.onopen = () => {
    // The SockJS open frame ("o") is delivered as the first server message;
    // STOMP CONNECT is sent only after we signal OPEN, mirroring WebSocket.
  };
  raw.onerror = (evt) => socket.onerror?.(evt);
  raw.onclose = (evt) => {
    socket.readyState = CLOSED;
    socket.onclose?.(evt);
  };
  raw.onmessage = (evt) => {
    // After a close frame the server closes the transport; ignore stragglers.
    if (socket.readyState === CLOSED) return;
    // Encoding differs by implementation: Spring's WebSocket transport sends
    // each datagram as a raw string ("o", "a[...]"), while the SockJS spec
    // wraps frames in a JSON array (["o"]). Normalize to a flat frame list.
    const datagram = String(evt.data);
    let frames;
    if (
      datagram === "o" ||
      datagram === "h" ||
      datagram.startsWith("c") ||
      datagram.startsWith("a")
    ) {
      frames = [datagram];
    } else {
      try {
        const parsed = JSON.parse(datagram);
        frames = Array.isArray(parsed) ? parsed : [datagram];
      } catch {
        frames = [datagram];
      }
    }
    for (const frame of frames) {
      if (!frame) continue;
      if (frame === "o") {
        if (socket.readyState === CONNECTING) {
          socket.readyState = OPEN;
          socket.onopen?.();
          // Deliver anything buffered before the transport opened.
          const outbox = socket._outbox.splice(0, socket._outbox.length);
          for (const data of outbox) socket._sendNow(data);
        }
      } else if (frame === "h") {
        // SockJS heartbeat — nothing to do.
      } else if (frame.startsWith("c")) {
        // SockJS close frame ("c[code]"). The server closes the underlying
        // WebSocket right after sending it, so we only cascade the close
        // through the transport — exactly ONE onclose event reaches the caller.
        socket.readyState = CLOSED;
        raw.close();
      } else if (frame.startsWith("a")) {
        // "a" = application messages: a[<json string>,<json string>,...].
        let messages;
        try {
          messages = JSON.parse(frame.slice(1));
        } catch {
          messages = [frame];
        }
        for (const message of Array.isArray(messages) ? messages : [messages]) {
          socket.onmessage?.({ data: unwrap(message) });
        }
      } else {
        socket.onmessage?.({ data: unwrap(frame) });
      }
    }
  };

  return socket;
}

/** A message inside an "a[...]" datagram is a JSON-encoded string. */
function unwrap(message) {
  try {
    const parsed = JSON.parse(message);
    return typeof parsed === "string" ? parsed : message;
  } catch {
    return message;
  }
}

class SockJsWebSocket {
  constructor(raw) {
    this._raw = raw;
    this.url = raw.url;
    // STOMP adapter reads binaryType; ours is a text transport.
    this.binaryType = "blob";
    this.readyState = CONNECTING;
    this._outbox = [];
    this.onopen = null;
    this.onmessage = null;
    this.onclose = null;
    this.onerror = null;
  }

  send(data) {
    if (this.readyState === CONNECTING) {
      // SockJS clients buffer sends issued before the open frame arrives;
      // STOMP CONNECT is typically sent right after transport creation.
      this._outbox.push(data);
      return;
    }
    this._sendNow(data);
  }

  _sendNow(data) {
    const asString =
      typeof data === "string"
        ? data
        : new TextDecoder("utf-8").decode(data);
    // Every client→server datagram is a JSON array of strings.
    this._raw.send(JSON.stringify([asString]));
  }

  close() {
    if (this.readyState === CLOSED) return;
    this.readyState = CLOSING;
    try {
      this._raw.close();
    } catch {
      // already closed
    }
  }

  terminate() {
    if (typeof this._raw.terminate === "function") {
      this._raw.terminate();
    } else {
      this.close();
    }
  }
}