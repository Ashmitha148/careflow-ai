import { Client, ReconnectionTimeMode } from "@stomp/stompjs";
import { createSockJsWebSocket } from "./sockJsWebSocket.js";

/**
 * STOMP-over-WebSocket client wrapper.
 *
 * Responsibilities:
 *  - Connect with a fresh JWT per attempt: {@code webSocketFactory} is invoked
 *    by the library for every (re)connect, so an expired access token during a
 *    long-lived session is never a problem — the HTTP request interceptor
 *    refreshes it in localStorage, and we read the latest value at connect time.
 *  - Automatic reconnection with truncated exponential backoff
 *    (reconnectDelay -> maxReconnectDelay, ReconnectionTimeMode.EXPONENTIAL)
 *    and STOMP heartbeats so dead connections are detected quickly.
 *  - Re-subscribe: the underlying @stomp/stompjs v7 StompHandler is recreated
 *    after a disconnect and does NOT restore subscriptions, so this wrapper
 *    buffers desired subscriptions and re-issues them on every onConnect.
 *  - SockJS fallback when raw WebSocket cannot be established.
 *
 * Domain payloads are routed by their ORIGINAL STOMP destination (the server
 * rewrites /user/queue/notifications internally, but messages carry our stable
 * subscription id, which encodes the external destination).
 */

const SUB_ID_PREFIX = "careflow+";
const RECONNECT_DELAY_MS = 1000; // doubles on each failed attempt
const MAX_RECONNECT_DELAY_MS = 30000; // cap the backoff
const HEARTBEAT_MS = 10000;

function parsePayload(body) {
  if (body == null || body === "") return null;
  try {
    return JSON.parse(body);
  } catch {
    return body;
  }
}

function buildUrl(getUrl, token) {
  const base = getUrl().replace(/\/+$/, "");
  const sep = base.includes("?") ? "&" : "?";
  return `${base}${sep}token=${encodeURIComponent(token || "")}`;
}

export class RealtimeSocketClient {
  constructor({ getToken, getUrl, onStatusChange, onMessage, enableSockJsFallback = true }) {
    this.getToken = getToken;
    this.getUrl = getUrl;
    this.onStatusChange = onStatusChange;
    this.onMessage = onMessage;
    this.enableSockJsFallback = enableSockJsFallback;

    this.client = null;
    this.desired = new Map(); // external destination -> Set<handler>
    this.pendingAppSends = []; // SENDs buffered while disconnected
    this.active = false;
    this.connected = false;
  }

  get isConnected() {
    return this.connected;
  }

  connect() {
    if (this.active) return;
    this.active = true;
    this.createClient(true);
  }

  createClient(initial) {
    if (!this.active) return;
    const self = this;

    this.client = new Client({
      connectionTimeout: 10000,
      reconnectDelay: RECONNECT_DELAY_MS,
      maxReconnectDelay: MAX_RECONNECT_DELAY_MS,
      reconnectTimeMode: ReconnectionTimeMode.EXPONENTIAL,
      heartbeatIncoming: HEARTBEAT_MS,
      heartbeatOutgoing: HEARTBEAT_MS,
      webSocketFactory: () => self.createWebSocket(),
    });

    this.client.onConnect = () => {
      this.connected = true;
      // Re-issue every desired subscription on this fresh connection.
      for (const destination of this.desired.keys()) {
        this.issueSubscription(destination);
      }
      // Flush any presence/admin SENDs buffered while offline.
      if (this.pendingAppSends.length > 0) {
        const pending = this.pendingAppSends;
        this.pendingAppSends = [];
        for (const send of pending) {
          this.forwardSend(send.destination, send.body);
        }
      }
      this.onStatusChange?.("connected");
    };

    this.client.onDisconnect = () => {
      this.connected = false;
      this.onStatusChange?.("disconnected");
    };
    this.client.onWebSocketClose = () => {
      this.connected = false;
      this.onStatusChange?.("disconnected");
    };
    this.client.onWebSocketError = () => {
      this.onStatusChange?.("disconnected");
    };
    this.client.onStompError = () => {
      this.onStatusChange?.("error");
    };

    try {
      this.client.activate();
    } catch {
      this.connected = false;
      this.onStatusChange?.("disconnected");
    }
    if (initial) this.onStatusChange?.("connecting");
  }

  createWebSocket() {
    const token = this.getToken();
    if (this.enableSockJsFallback) {
      // The CareFlow backend registers /ws with .withSockJS(): the endpoint
      // serves the SockJS protocol at /ws/{server}/{session}/websocket and a
      // raw STOMP WebSocket at /ws is rejected. Connect via the SockJS
      // transport immediately — no wasted failed upgrade round trip.
      return createSockJsWebSocket(this.getUrl(), token);
    }
    // Native-STOMP servers (no SockJS): plain WebSocket with the token in the
    // query string.
    return new WebSocket(buildUrl(this.getUrl, token));
  }

  /** Registers a handler for a STOMP destination. */
  subscribe(destination, handler) {
    if (!this.desired.has(destination)) this.desired.set(destination, new Set());
    this.desired.get(destination).add(handler);
    if (this.connected) {
      this.issueSubscription(destination);
    }
  }

  /** Removes a handler; unsubscribes the STOMP topic when the last one goes. */
  unsubscribe(destination, handler) {
    const handlers = this.desired.get(destination);
    if (!handlers) return;
    handlers.delete(handler);
    if (handlers.size === 0) {
      this.desired.delete(destination);
      this.sendUnsubscribe(destination);
    }
  }

  issueSubscription(destination) {
    if (!this.client || !this.connected) return;
    try {
      this.client.subscribe(
        destination,
        (frame) => this.route(destination, frame),
        { id: SUB_ID_PREFIX + destination },
      );
    } catch {
      // Connection dropped mid-call; onConnect re-issues everything.
    }
  }

  sendUnsubscribe(destination) {
    if (!this.client || !this.connected) return;
    try {
      this.client.unsubscribe(SUB_ID_PREFIX + destination);
    } catch {
      // Ignore — the subscription may already be gone with the connection.
    }
  }

  route(destination, frame) {
    const handlers = this.desired.get(destination);
    if (!handlers) return;
    const payload = parsePayload(frame.body);
    for (const handler of handlers) {
      try {
        handler(payload, frame);
      } catch {
        // A handling bug must not kill the socket router.
      }
    }
  }

  /** Sends to a STOMP app/queue destination (e.g. /app/presence/...). */
  send(destination, body) {
    if (this.connected) {
      return this.forwardSend(destination, body);
    }
    // Buffer so presence JOIN/LEAVE still lands when the connection returns.
    this.pendingAppSends.push({ destination, body });
    if (this.pendingAppSends.length > 50) {
      this.pendingAppSends.splice(0, this.pendingAppSends.length - 50);
    }
    return false;
  }

  forwardSend(destination, body) {
    if (!this.client || !this.connected) return false;
    const asString = typeof body === "string" ? body : JSON.stringify(body);
    try {
      this.client.publish({ destination, body: asString });
      return true;
    } catch {
      return false;
    }
  }

  disconnect() {
    this.active = false;
    this.connected = false;
    this.desired.clear();
    this.pendingAppSends = [];
    if (this.client) {
      try {
        this.client.deactivate({ force: true });
      } catch {
        // already torn down
      }
      this.client = null;
    }
    this.onStatusChange?.("disconnected");
  }
}