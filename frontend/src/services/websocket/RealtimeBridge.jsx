import { useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useAuth } from "../../context/AuthContext";
import { useSocket } from "../../context/SocketContext";
import { WS_CHANNELS } from "./realtimeChannels";
import { handleNotificationEvent, handleTaskEvent } from "./realtimeHandlers";
import { QK } from "../queryKeys";

/**
 * Global socket → TanStack Query bridge.
 *
 * Subscribes to the destinations that apply to EVERY signed-in session
 * (private notifications + the current user's nurse queue) and applies each
 * message to the query cache with setQueryData — the exact keys the data hooks
 * read. Patient-scoped topics (timeline/vitals/presence) are subscribed by the
 * patient workspace page, which knows which patient is open.
 */
export default function RealtimeBridge() {
  const { user } = useAuth();
  const { subscribe } = useSocket();
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!user?.id) return undefined;

    const disposers = [
      subscribe(WS_CHANNELS.notifications(), (notification) => {
        handleNotificationEvent(queryClient, notification, user.id);
      }),
    ];

    if (user.role === "NURSE") {
      disposers.push(
        subscribe(WS_CHANNELS.nurseQueue(user.id), (task) => {
          handleTaskEvent(queryClient, task);
          // Task assignment is also a notification; the queue message is the
          // authoritative record, but refresh the unread badge from the server
          // occasionally in case socket and DB ever disagree.
          if (task) {
            queryClient.invalidateQueries({ queryKey: QK.unreadCount(user.id) });
          }
        }),
      );
    }

    return () => disposers.forEach((dispose) => dispose());
  }, [user?.id, user?.role, subscribe, queryClient]);

  return null;
}