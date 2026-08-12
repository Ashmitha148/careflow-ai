import api from "./api";

export async function createTask({ patientId, doctorId, assignedNurseId, title, description, dueAt }) {
  const response = await api.post("/tasks", null, {
    params: { patientId, doctorId, assignedNurseId, title, description, dueAt },
  });
  return response.data;
}

export async function startTask(taskId, nurseId) {
  const response = await api.post(`/tasks/${taskId}/start`, null, { params: { nurseId } });
  return response.data;
}

export async function completeTask(taskId, nurseId) {
  const response = await api.post(`/tasks/${taskId}/complete`, null, { params: { nurseId } });
  return response.data;
}

export async function cancelTask(taskId, userId) {
  const response = await api.post(`/tasks/${taskId}/cancel`, null, { params: { userId } });
  return response.data;
}

export async function getTasksForPatient(patientId) {
  const response = await api.get(`/tasks/patient/${patientId}`);
  return response.data;
}

export async function getTasksForNurse(nurseId) {
  const response = await api.get(`/tasks/nurse/${nurseId}`);
  return response.data;
}

export async function getPendingTasksForNurse(nurseId) {
  const response = await api.get(`/tasks/nurse/${nurseId}/pending`);
  return response.data;
}
