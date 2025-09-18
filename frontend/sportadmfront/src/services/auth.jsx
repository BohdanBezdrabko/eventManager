// src/services/auth.jsx
const API_ROOT = import.meta.env.VITE_API_URL || "http://localhost:8081";

// === Token API (single source of truth) ===
const ACCESS_KEY = "accessToken";

/** Persist JWT access token */
export function setToken(token) {
    if (!token) return;
    localStorage.setItem(ACCESS_KEY, token);
}

/** Read JWT access token */
export function getToken() {
    return localStorage.getItem(ACCESS_KEY);
}

/** Clear all auth state on client */
export function clearToken() {
    localStorage.removeItem(ACCESS_KEY);
}

/** Helper: JSON fetch with base URL and auth header */
async function jsonFetch(path, init = {}) {
    const url = /^https?:\/\//i.test(path) ? path : `${API_ROOT}${path}`;

    const headers = new Headers(init.headers || {});
    if (!headers.has("Accept")) headers.set("Accept", "application/json");
    if (init.body && !(init.body instanceof FormData) && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }

    const token = getToken();
    if (token && !headers.has("Authorization")) {
        headers.set("Authorization", `Bearer ${token}`);
    }

    const res = await fetch(url, { ...init, headers });

    if (res.status === 204) return null;

    const text = await res.text();
    const data = text ? (() => { try { return JSON.parse(text); } catch { return text; } })() : null;

    if (!res.ok) {
        if (res.status === 401) clearToken();
        const message = (data && data.message) || res.statusText || "Request failed";
        throw new Error(message);
    }

    return data;
}

// === Auth endpoints ===
export async function loginUser({ username, email, password }) {
    const payload = username ? { username, password } : { email, password };
    const data = await jsonFetch("/api/v1/auth/login", {
        method: "POST",
        body: JSON.stringify(payload),
    });

    const accessToken = data?.accessToken || data?.token || data?.jwt || data?.id_token;
    if (accessToken) setToken(accessToken);

    const user = data?.user ?? null;
    return { user, accessToken };
}

export async function registerUser(payload) {
    const data = await jsonFetch("/api/v1/auth/register", {
        method: "POST",
        body: JSON.stringify(payload),
    });
    const accessToken = data?.accessToken || data?.token || null;
    if (accessToken) setToken(accessToken);
    return data;
}

export async function logoutUser() {
    try {
        await jsonFetch("/api/v1/auth/logout", { method: "POST" });
    } catch (_) {
        // ignore
    } finally {
        clearToken();
    }
}
