import api from "./api";

const TOKEN_KEY = "careflow_token";
const REFRESH_TOKEN_KEY = "careflow_refresh_token";

function storeTokens(data) {
  localStorage.setItem(TOKEN_KEY, data.token);
  if (data.refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken);
  }
}

export async function login(email, password) {
  const response = await api.post("/auth/login", { email, password });
  storeTokens(response.data);
  return response.data.user;
}

export async function register(email, password, fullName, role) {
  const response = await api.post("/auth/register", {
    email,
    password,
    fullName,
    role,
  });
  storeTokens(response.data);
  return response.data.user;
}

export async function refreshAccessToken() {
  const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
  if (!refreshToken) {
    throw new Error("No refresh token available");
  }
  const response = await api.post("/auth/refresh", { refreshToken });
  storeTokens(response.data);
  return response.data.user;
}

export async function getCurrentUser() {
  const response = await api.get("/auth/me");
  return response.data;
}

export function logout() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}
