import api from "./api";

export async function getPatientVitals(patientId) {
  const response = await api.get(`/clinical/vitals/${patientId}`);
  return response.data;
}

export async function getPatientMedications(patientId) {
  const response = await api.get(`/clinical/medications/${patientId}`);
  return response.data;
}

export async function getPatientAppointments(patientId) {
  const response = await api.get(`/clinical/appointments/${patientId}`);
  return response.data;
}

export async function getPatientFiles(patientId) {
  const response = await api.get(`/clinical/files/${patientId}`);
  return response.data;
}
