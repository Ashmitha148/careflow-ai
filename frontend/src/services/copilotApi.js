import api from "./api";

const textBody = { headers: { "Content-Type": "text/plain" } };

export async function askTimelineQuestion(patientId, question) {
  const response = await api.post("/copilot/timeline-question", question, {
    params: { patientId },
    ...textBody,
  });
  return response.data;
}

export async function getPatientSummary(patientId) {
  const response = await api.post("/copilot/patient-summary", null, {
    params: { patientId },
  });
  return response.data;
}

export async function getPlainLanguage(patientId, text) {
  const response = await api.post("/copilot/plain-language", text, {
    params: { patientId },
    ...textBody,
  });
  return response.data;
}

export async function getPatientPromptHistory(patientId) {
  const response = await api.get(`/copilot/patient/${patientId}/history`);
  return response.data;
}

export async function getUserPromptHistory(userId) {
  const response = await api.get(`/copilot/user/${userId}/history`);
  return response.data;
}
