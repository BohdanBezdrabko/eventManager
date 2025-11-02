// src/context/AuthContext.jsx
import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { getMe, loginUser, registerUser, setToken, clearToken, getToken, userFromToken } from "@/services/auth.jsx";

const AuthContext = createContext(null);

/**
 * Контекст авторизації:
 * - booting: true поки ініціалізуємо стан (LS або /me)
 * - user: { id, username, roles } | null
 * - login(username, password)
 * - register(username, password, role?, extra?)
 * - logout()
 */
export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [booting, setBooting] = useState(true);

    // Ініціалізація при старті (F5): відновити токен => запитати /me
    useEffect(() => {
        (async () => {
            try {
                const t = getToken();
                if (!t) {
                    setUser(null);
                    return;
                }
                // На випадок, якщо /me падає, спробуємо хоча б відобразити username із токена.
                const weakUser = userFromToken(t);
                if (weakUser && !weakUser.id) setUser(weakUser);

                const me = await getMe(); // { id, username, roles }
                if (me && me.id) setUser(me);
            } catch {
                setUser(null);
            } finally {
                setBooting(false);
            }
        })();
    }, []);

    // Публічні методи
    async function login(username, password) {
        const { token, user: u } = await loginUser(username, password);
        setToken(token);
        setUser(u || null);
        return { token, user: u };
    }

    async function register(username, password, role = "user", extra = {}) {
        const { token, user: u } = await registerUser(username, password, role, extra);
        if (token) setToken(token);
        setUser(u || null);
        return { token, user: u };
    }

    function logout() {
        clearToken();
        setUser(null);
    }

    const value = useMemo(
        () => ({ user, booting, login, register, logout }),
        [user, booting]
    );

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error("useAuth must be used within AuthProvider");
    return ctx;
}
