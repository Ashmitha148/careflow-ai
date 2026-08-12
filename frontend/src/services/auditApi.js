import api from "./api";

export async function getAuditLogs(page = 0, size = 20) {
  const response = await api.get("/audit-logs", { params: { page, size } });
  return response.data;
}
