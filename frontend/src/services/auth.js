import api from "./api";

export async function login(email, password) {
  const response = await api.post("/auth/login", { email, password });
  localStorage.setItem("careflow_token", response.data.token);
  return response.data.user;
}

export async function getCurrentUser() {
  const response = await api.get("/auth/me");
  return response.data;
}

export function logout() {
  localStorage.removeItem("careflow_token");
}
