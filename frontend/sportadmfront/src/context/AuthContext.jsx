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
        }
        setBooting(false);
    }, []);

    async function login(username, password) {
        const { token, user } = await loginUser(username, password);
        if (token) setToken(token);
        const u = user || userFromToken(token);
        setUser(u || null);
        return u;
    }

    async function register(username, password, role) {
        const { token, user } = await registerUser(username, password, role);
        if (token) setToken(token);
        const u = user || userFromToken(token);
        setUser(u || null);
        return u;
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
