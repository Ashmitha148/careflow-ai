import { createContext, useContext, useEffect, useState } from "react";
import { login as loginRequest, logout as logoutRequest, getCurrentUser } from "../services/auth";

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem("careflow_token");
    if (!token) {
      setLoading(false);
      return;
    }
    getCurrentUser()
      .then((me) => setUser(me))
      .catch(() => {
        localStorage.removeItem("careflow_token");
        setUser(null);
      })
      .finally(() => setLoading(false));
  }, []);

  async function login(email, password) {
    const loggedInUser = await loginRequest(email, password);
    setUser(loggedInUser);
    return loggedInUser;
  }

  function logout() {
    logoutRequest();
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}
