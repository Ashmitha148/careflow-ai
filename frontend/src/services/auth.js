import api from "./api";
import { MOCK_USERS } from "./mockDemoData";

const TOKEN_KEY = "careflow_token";
const REFRESH_TOKEN_KEY = "careflow_refresh_token";

function storeTokens(data) {
  localStorage.setItem(TOKEN_KEY, data.token);
  if (data.refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken);
  }
}

export async function login(email, password) {
  try {
    const response = await api.post("/auth/login", { email, password });
    storeTokens(response.data);
    if (response.data.user) {
      localStorage.setItem("careflow_user", JSON.stringify(response.data.user));
    }
    return response.data.user;
  } catch (err) {
    // If backend is sleeping/unreachable or times out, seamlessly authenticate demo users
    const mockUser = MOCK_USERS[email];
    if (mockUser) {
      const demoData = {
        token: "demo-jwt-token-" + (mockUser.role || "DOCTOR").toLowerCase(),
        refreshToken: "demo-refresh-token-" + (mockUser.role || "DOCTOR").toLowerCase(),
        user: { ...mockUser },
      };
      storeTokens(demoData);
      localStorage.setItem("careflow_user", JSON.stringify(demoData.user));
      return demoData.user;
    }
    throw err;
  }
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
