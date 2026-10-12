import axios from "axios";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";

const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("careflow_token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// Silent refresh: on a 401, try exchanging the refresh token for a new
// access token exactly once, then retry the original request. If that
// fails too (refresh token expired/invalid), fall back to a hard logout.
let refreshPromise = null;

import {
  MOCK_USERS,
  MOCK_PATIENTS,
  MOCK_VITALS,
  MOCK_MEDICATIONS,
  MOCK_TASKS,
  MOCK_TIMELINE,
  MOCK_DASHBOARD_SUMMARY,
} from "./mockDemoData";

function getMockResponseForRequest(config) {
  const url = config.url || "";
  const method = (config.method || "get").toLowerCase();

  if (url.includes("/auth/login") && method === "post") {
    let body = config.data;
    if (typeof body === "string") {
      try { body = JSON.parse(body); } catch (_) {}
    }
    const email = body?.email;
    const user = MOCK_USERS[email] || MOCK_USERS["doctor1@careflow.ai"];
    return {
      token: "demo-jwt-token-" + (user.role || "DOCTOR").toLowerCase(),
      refreshToken: "demo-refresh-token-" + (user.role || "DOCTOR").toLowerCase(),
      user: { ...user },
    };
  }

  if (url.includes("/auth/me")) {
    const savedUser = localStorage.getItem("careflow_user");
    if (savedUser) {
      try { return JSON.parse(savedUser); } catch (_) {}
    }
    return MOCK_USERS["doctor1@careflow.ai"];
  }

  if (url.includes("/dashboard/summary")) {
    return MOCK_DASHBOARD_SUMMARY;
  }

  if (url.includes("/patients/my") || url === "/patients" || url.startsWith("/patients?")) {
    return {
      content: MOCK_PATIENTS,
      totalElements: MOCK_PATIENTS.length,
      totalPages: 1,
      size: 20,
      number: 0,
    };
  }

  if (url.match(/\/patients\/[^/]+$/) && method === "get") {
    return MOCK_PATIENTS[0];
  }

  if (url.includes("/clinical/vitals")) {
    return MOCK_VITALS;
  }

  if (url.includes("/clinical/medications")) {
    return MOCK_MEDICATIONS;
  }

  if (url.includes("/tasks")) {
    if (method === "get") return MOCK_TASKS;
    if (method === "post") return MOCK_TASKS[0];
  }

  if (url.includes("/timeline")) {
    return {
      content: MOCK_TIMELINE,
      totalElements: MOCK_TIMELINE.length,
      hasMore: false,
    };
  }

  if (url.includes("/copilot")) {
    return {
      summary: "Patient Eleanor Vance (78yo F) is admitted with Congestive Heart Failure (NYHA Class III). Latest vitals show sinus tachycardia at 112 bpm with elevated BP 148/92 mmHg. Administered Lisinopril 10mg; IV Furosemide pending. Remote supervision enabled.",
      promptId: "prompt-demo-001",
      groundingEventIds: ["t1111111-1111-1111-1111-111111111111", "t2222222-2222-2222-2222-222222222222"],
    };
  }

  return null;
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const isNetworkOrServerDown =
      !error.response ||
      error.response.status === 502 ||
      error.response.status === 503 ||
      error.response.status === 504 ||
      error.code === "ECONNABORTED" ||
      error.message?.includes("Network Error");

    // If backend is waking up or down, fall back to offline demo data seamlessly
    if (isNetworkOrServerDown && originalRequest) {
      const mockData = getMockResponseForRequest(originalRequest);
      if (mockData !== null) {
        if (originalRequest.url?.includes("/auth/login") && mockData.user) {
          localStorage.setItem("careflow_user", JSON.stringify(mockData.user));
        }
        return Promise.resolve({
          data: mockData,
          status: 200,
          statusText: "OK (Offline Demo Fallback)",
          headers: {},
          config: originalRequest,
        });
      }
    }

    const isAuthEndpoint = originalRequest?.url?.startsWith("/auth/");

    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !isAuthEndpoint &&
      window.location.pathname !== "/login"
    ) {
      const refreshToken = localStorage.getItem("careflow_refresh_token");

      if (!refreshToken) {
        localStorage.removeItem("careflow_token");
        window.location.href = "/login";
        return Promise.reject(error);
      }

      originalRequest._retry = true;

      try {
        if (!refreshPromise) {
          refreshPromise = axios
            .post(`${API_BASE_URL}/auth/refresh`, { refreshToken })
            .finally(() => {
              refreshPromise = null;
            });
        }

        const { data } = await refreshPromise;
        localStorage.setItem("careflow_token", data.token);
        localStorage.setItem("careflow_refresh_token", data.refreshToken);

        originalRequest.headers.Authorization = `Bearer ${data.token}`;
        return api(originalRequest);
      } catch (refreshError) {
        localStorage.removeItem("careflow_token");
        localStorage.removeItem("careflow_refresh_token");
        window.location.href = "/login";
        return Promise.reject(refreshError);
      }
    }

    // Handle 409 Conflict (e.g. concurrent edits / optimistic locking version mismatch)
    if (error.response?.status === 409) {
      const conflictMsg = error.response.data?.message || "This record was updated by another team member. Please refresh to see the latest changes.";
      error.conflictMessage = conflictMsg;
    }

    return Promise.reject(error);
  },
);

export default api;
