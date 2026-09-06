import { useEffect, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useSocket } from "../../context/SocketContext";
import { WS_CHANNELS, PRESENCE_ACTIONS } from "./realtimeChannels";
import {
  extractViewers,
  handleTaskEvent,
  handleTimelineEvent,
  handleVitalEvent,
} from "./realtimeHandlers";

/**
 * Patient-scoped realtime wiring for a workspace screen.
 *
 * Subscribes the STOMP destinations that only make sense once we know WHICH
 * patient is open:
 *   /topic/patient/{id}/timeline — new append-only events → TanStack cache
 *   /topic/patient/{id}/vitals   — live vitals stream → TanStack cache
 *   /topic/patient/{id}/presence — live presence snapshots → { viewers }
 *
 * Presence lifecycle:
 *   subscribe (mount) → JOIN → server broadcasts the full viewer list
 *   unsubscribe (unmount) → LEAVE → server rebroadcasts without us
 *   disconnect → PresenceDisconnectListener removes the dead session server-side
 *
 * Sends are buffered by the socket client while offline and flushed on the
 * next reconnect, and the effect re-runs when the connection state flips so a
 * fresh JOIN is emitted for each new session.
 */
export function usePatientRealtime(patientId) {
  const { subscribe, send, connected } = useSocket();
  const queryClient = useQueryClient();
  const [viewers, setViewers] = useState([]);

  useEffect(() => {
    if (!patientId) return undefined;

    // On (re)connect we have no way to know who else is still watching until
    // the server tells us — start from an empty slate per session.
    setViewers([]);

    const disposers = [
      subscribe(WS_CHANNELS.timeline(patientId), (event) =>
        handleTimelineEvent(queryClient, event)),
      subscribe(WS_CHANNELS.vitals(patientId), (vital) =>
        handleVitalEvent(queryClient, vital)),
      subscribe(WS_CHANNELS.presence(patientId), (payload) =>
        setViewers(extractViewers(payload))),
      subscribe(WS_CHANNELS.patientTasks(patientId), (task) =>
        handleTaskEvent(queryClient, task)),
    ];

    if (connected) {
      // STOMP processes frames per connection in order, so by the time this
      // SEND is handled the SUBSCRIBE above is registered and we receive the
      // broadcast.
      send(WS_CHANNELS.presenceControl(patientId), {
        action: PRESENCE_ACTIONS.JOIN,
      });
    }

    return () => {
      if (connected) {
        send(WS_CHANNELS.presenceControl(patientId), {
          action: PRESENCE_ACTIONS.LEAVE,
        });
      }
      disposers.forEach((dispose) => dispose());
    };
  }, [patientId, connected, subscribe, send, queryClient]);

  return { viewers };
}