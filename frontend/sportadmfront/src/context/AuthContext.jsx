// src/context/AuthContext.jsx
import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { loginUser, registerUser, setToken, getToken, clearToken, userFromToken } from "@/services/auth.jsx";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [booting, setBooting] = useState(true);

    useEffect(() => {
        const t = getToken();
        if (t) {
            const u = userFromToken(t);
            const now = Math.floor(Date.now() / 1000);
            if (u?.exp && u.exp < now) {
                clearToken();
                setUser(null);
            } else {
                setUser(u || null);
            }
        } else {
            setUser(null);
        }
        setBooting(false);
    }, []);

    async function login(identifier, password) {
        const { token, user } = await loginUser(identifier, password);
        setToken(token);
        setUser(user);
        return user;
    }

    async function register(username, password, role = "user", extra = {}) {
        const { token, user } = await registerUser(username, password, role, extra);
        if (token) setToken(token);
        setUser(user);
        return user;
    }

    function logout() {
        clearToken();
        setUser(null);
    }

    const value = useMemo(() => ({ user, booting, login, register, logout }), [user, booting]);
    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error("useAuth must be used within AuthProvider");
    return ctx;
}
