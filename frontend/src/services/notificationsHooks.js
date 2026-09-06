import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { getNotifications, getUnreadCount } from "./notificationApi";
import { QK } from "./queryKeys";
import { useStore } from "../store/useStore";

export function useUnreadCount(userId) {
  const query = useQuery({
    queryKey: QK.unreadCount(userId),
    queryFn: () => getUnreadCount(userId),
    enabled: Boolean(userId),
    refetchInterval: 60_000,
  });

  // Hydrate the store badge used by the topbar with the authoritative count.
  const setUnreadNotifications = useStore((s) => s.setUnreadNotifications);
  useEffect(() => {
    if (typeof query.data === "number") {
      setUnreadNotifications(query.data);
    }
  }, [query.data, setUnreadNotifications]);

  return query;
}

export function useNotifications(userId) {
  return useQuery({
    queryKey: QK.notifications(userId),
    queryFn: () => getNotifications(userId),
    enabled: Boolean(userId),
    staleTime: 10_000,
  });
}