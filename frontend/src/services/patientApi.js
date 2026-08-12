import api from "./api";

export async function searchPatients(query = "", page = 0, size = 20) {
  const response = await api.get("/patients", {
    params: {
      query: query || undefined,
      page,
      size,
    },
  });

  return response.data;
}

export async function getPatient(patientId) {
  const response = await api.get(`/patients/${patientId}`);
  return response.data;
}
