import api from "./api";

export async function getPatientTimeline(patientId, params = {}) {
  const response = await api.get(`/patients/${patientId}/timeline`, {
    params,
  });
  return response.data;
}

export async function getTimelineReplay(patientId, range = "24h") {
  const response = await api.get(
    `/patients/${patientId}/timeline/replay/${range}`
  );
  return response.data;
}
