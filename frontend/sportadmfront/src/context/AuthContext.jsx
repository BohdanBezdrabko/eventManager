// src/context/AuthContext.jsx
import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import {
    loginUser,
    registerUser,
    logoutUser,
    getToken,
    setToken,
    clearToken,
} from '../services/auth';

function parseJwt(token) {
    try {
        const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
        const json = decodeURIComponent(
            atob(base64)
                .split('')
                .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                .join('')
        );
        return JSON.parse(json);
    } catch {
        return null;
    }
}

function mapPayloadToUser(p) {
    if (!p) return null;
    return {
        id: p.id ?? p.userId ?? p.sub ?? null,
        username: p.username ?? p.sub ?? '',
        roles: Array.isArray(p.roles) ? p.roles : (Array.isArray(p.authorities) ? p.authorities : []),
    };
}

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [booting, setBooting] = useState(true);

    // Ініціалізація з localStorage токена
    useEffect(() => {
        const t = getToken?.();
        if (!t) {
            setBooting(false);
            return;
        }
        const u = mapPayloadToUser(parseJwt(t));
        if (u) setUser(u);
        else {
            clearToken?.();
            setUser(null);
        }
        setBooting(false);
    }, []);

    const value = useMemo(
        () => ({
            user,
            booting,

            // Логін: сервіс повертає { token, user }
            async login(credentials) {
                const { token, user: uFromApi } = await loginUser(credentials);
                if (token) setToken(token);
                const u = uFromApi ?? mapPayloadToUser(parseJwt(token));
                setUser(u ?? null);
                return u ?? null;
            },

            // Реєстрація: сервіс повертає { token, user }
            async register(payload) {
                const { token, user: uFromApi } = await registerUser(payload);
                if (token) setToken(token);
                const u = uFromApi ?? mapPayloadToUser(parseJwt(token));
                setUser(u ?? null);
                return u ?? null;
            },

            async logout() {
                try {
                    await logoutUser?.();
                } finally {
                    clearToken?.();
                    setUser(null);
                }
            },
        }),
        [user, booting]
    );

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error('useAuth must be used within AuthProvider');
    return ctx;
}
