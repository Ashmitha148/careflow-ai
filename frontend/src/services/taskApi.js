import api from "./api";

export async function createTask({ patientId, assignedNurseId, title, description, dueAt }) {
  const response = await api.post("/tasks", null, {
    params: { patientId, assignedNurseId, title, description, dueAt },
  });
  return response.data;
}

export async function startTask(taskId) {
  const response = await api.post(`/tasks/${taskId}/start`);
  return response.data;
}

export async function completeTask(taskId) {
  const response = await api.post(`/tasks/${taskId}/complete`);
  return response.data;
}

export async function cancelTask(taskId) {
  const response = await api.post(`/tasks/${taskId}/cancel`);
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
