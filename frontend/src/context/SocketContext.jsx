import { createContext, useCallback, useContext, useEffect, useRef, useState } from "react";
import { RealtimeSocketClient } from "../services/websocket/socketClient";
import { useAuth } from "./AuthContext";
import { useStore } from "../store/useStore";

export const SocketContext = createContext(null);

/**
 * Builds the STOMP base URL from the API base URL (which already includes the
 * /api context-path): http -> ws, append /ws for the STOMP endpoint. Browser
 * WebSocket URLs may not carry custom headers, so the JWT is attached as a
 * query parameter and validated server-side by JwtHandshakeInterceptor.
 */
function getWebSocketBaseUrl() {
  const envUrl = import.meta.env.VITE_API_BASE_URL;
  const base = envUrl && !envUrl.startsWith("/")
    ? envUrl
    : `${window.location.origin}/api`;
  return base.replace(/\/+$/, "") + "/ws";
}

/**
 * Owns a single STOMP connection for the signed-in user and fans incoming
 * messages out to per-destination handlers. Reconnecting, re-subscribing and
 * token refresh are handled by RealtimeSocketClient.
 */
export function SocketProvider({ children }) {
  const { user } = useAuth();
  const { setRealtimeStatus } = useStore();

  const [status, setStatusState] = useState("disconnected");
  const [connected, setConnected] = useState(false);

  const clientRef = useRef(null);
  const routesRef = useRef(new Map()); // destination -> { handlers: Map<hid,fn>, router }
  const routeCounterRef = useRef(0);

  const setStatus = useCallback((next) => {
    setStatusState(next);
    setConnected(next === "connected");
    setRealtimeStatus(next);
  }, [setRealtimeStatus]);

  // Connect on login, tear down on logout.
  useEffect(() => {
    if (!user?.id) {
      if (clientRef.current) {
        clientRef.current.disconnect();
        clientRef.current = null;
      }
      routesRef.current.clear();
      setStatus("disconnected");
      return undefined;
    }

    if (!clientRef.current) {
      clientRef.current = new RealtimeSocketClient({
        getToken: () => localStorage.getItem("careflow_token"),
        getUrl: getWebSocketBaseUrl,
        onStatusChange: setStatus,
      });
      // Re-issue any subscriptions registered before the client existed
      // (child effects run before parent effects on mount).
      for (const [destination, entry] of routesRef.current) {
        clientRef.current.subscribe(destination, entry.router);
      }
    }
    clientRef.current.connect();

    return undefined;
  }, [user?.id, setStatus]);

  /**
   * Subscribe a handler to a STOMP destination.
   * Returns a disposer that unsubscribes this handle only.
   */
  const subscribe = useCallback((destination, handler) => {
    let entry = routesRef.current.get(destination);
    if (!entry) {
      entry = { handlers: new Map(), router: null };
      entry.router = (payload, frame) => {
        for (const handler of entry.handlers.values()) {
          try {
            handler(payload, frame);
          } catch {
            // A page-level handling bug must not break the router.
          }
        }
      };
      routesRef.current.set(destination, entry);
      clientRef.current?.subscribe(destination, entry.router);
    }

    const handlerId = ++routeCounterRef.current;
    entry.handlers.set(handlerId, handler);

    return () => {
      const current = routesRef.current.get(destination);
      if (!current) return;
      current.handlers.delete(handlerId);
      if (current.handlers.size === 0) {
        routesRef.current.delete(destination);
        clientRef.current?.unsubscribe(destination, current.router);
      }
    };
  }, []);

  /** Sends a STOMP SEND frame (e.g. presence control to /app/...). */
  const send = useCallback((destination, body) => {
    return clientRef.current ? clientRef.current.send(destination, body) : false;
  }, []);

  return (
    <SocketContext.Provider
      value={{ status, connected, subscribe, send, user }}
    >
      {children}
    </SocketContext.Provider>
  );
}

export function useSocket() {
  const ctx = useContext(SocketContext);
  if (!ctx) {
    throw new Error("useSocket must be used within a SocketProvider");
  }
  return ctx;
}