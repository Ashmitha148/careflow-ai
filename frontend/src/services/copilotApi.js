import api from "./api";

const textBody = { headers: { "Content-Type": "text/plain" } };

export async function askTimelineQuestion(patientId, userId, question) {
  const response = await api.post("/copilot/timeline-question", question, {
    params: { patientId, userId },
    ...textBody,
  });
  return response.data;
}

export async function getPatientSummary(patientId, userId) {
  const response = await api.post("/copilot/patient-summary", null, {
    params: { patientId, userId },
  });
  return response.data;
}

export async function getPlainLanguage(patientId, userId, text) {
  const response = await api.post("/copilot/plain-language", text, {
    params: { patientId, userId },
    ...textBody,
  });
  return response.data;
}
