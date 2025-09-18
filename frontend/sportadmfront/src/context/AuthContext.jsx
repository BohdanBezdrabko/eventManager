// src/context/AuthContext.jsx
import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { loginUser, registerUser, logoutUser, getToken, setToken, clearToken } from "@/services/auth";

const AuthContext = createContext(null);

// Minimal, robust JWT parser
function parseJwt(token) {
    try {
        const base64 = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
        const json = decodeURIComponent(
            atob(base64).split("").map(c => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2)).join("")
        );
        return JSON.parse(json);
    } catch {
        return null;
    }
}

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [booting, setBooting] = useState(true);

    // Rehydrate from token on load
    useEffect(() => {
        const token = getToken();
        if (!token) {
            setBooting(false);
            return;
        }

        const claims = parseJwt(token);
        if (claims) {
            const roles = Array.isArray(claims.roles)
                ? claims.roles
                : typeof claims.role === "string"
                    ? claims.role.split(",")
                    : typeof claims.scope === "string"
                        ? claims.scope.split(" ")
                        : [];
            setUser({
                id: claims.sub || claims.userId || null,
                username: claims.username || claims.preferred_username || claims.email || null,
                roles,
                claims,
            });
        }
        setBooting(false);
    }, []);

    const value = useMemo(() => ({
            user,
            booting,
            isAuthenticated: !!user,
            async login(credentials) {
                const { user: backendUser, accessToken } = await loginUser(credentials);
                if (accessToken) {
                    setToken(accessToken);
                    const claims = parseJwt(accessToken);
                    if (claims) {
                        const roles = Array.isArray(claims.roles)
                            ? claims.roles
                            : typeof claims.role === "string"
                                ? claims.role.split(",")
                                : [];
                        setUser(backendUser ?? {
                            id: claims.sub || claims.userId || null,
                            username: claims.username || claims.preferred_username || claims.email || null,
                            roles,
                            claims,
                        });
                        return;
                    }
                }
                if (backendUser) setUser(backendUser);
            },
            async register(payload) {
                return registerUser(payload);
            },
            async logout() {
                await logoutUser();
                setUser(null);
                clearToken();
            },
        }),
        [user, booting]
    );

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error("useAuth must be used within AuthProvider");
    return ctx;
}
