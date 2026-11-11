import api from "./api";

export async function getPatientVitals(patientId) {
  const response = await api.get(`/clinical/vitals/${patientId}`);
  return response.data;
}

export async function recordVital(vitalData) {
  const response = await api.post("/clinical/vitals", vitalData);
  return response.data;
}

export async function getPatientMedications(patientId) {
  const response = await api.get(`/clinical/medications/${patientId}`);
  return response.data;
}

export async function createMedication(medicationData) {
  const response = await api.post("/clinical/medications", medicationData);
  return response.data;
}

export async function recordMedicationAdministration(adminData) {
  const response = await api.post("/clinical/medications/administer", adminData);
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
