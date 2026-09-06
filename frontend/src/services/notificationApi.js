import api from "./api";

export async function getNotifications(userId) {
  const response = await api.get(`/notifications/user/${userId}`);
  return response.data;
}

export async function getUnreadCount(userId) {
  const response = await api.get(`/notifications/user/${userId}/unread/count`);
  return response.data;
}

export async function markNotificationRead(notificationId, userId) {
  const response = await api.patch(`/notifications/${notificationId}/read`, null, {
    params: { userId },
  });
  return response.data;
}

export async function markAllNotificationsRead(userId) {
  const response = await api.post(`/notifications/user/${userId}/read-all`);
  return response.data;
}