// src/services/auth.jsx
const API_ROOT = (import.meta.env.VITE_API_URL || "http://localhost:8081/api/v1").replace(/\/+$/, "");
const ACCESS_KEY = "accessToken";

function url(path) {
    return /^https?:\/\//i.test(path) ? path : `${API_ROOT}${path.startsWith("/") ? path : `/${path}`}`;
}

// storage API
export function setToken(token) {
    if (token) localStorage.setItem(ACCESS_KEY, token);
}
export function getToken() {
    return localStorage.getItem(ACCESS_KEY);
}
export function clearToken() {
    localStorage.removeItem(ACCESS_KEY);
}

// jwt -> user
function b64urlDecode(s) {
    try {
        const pad = "=".repeat((4 - (s.length % 4)) % 4);
        return decodeURIComponent(
            atob((s + pad).replace(/-/g, "+").replace(/_/g, "/"))
                .split("")
                .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
                .join("")
        );
    } catch {
        return "{}";
    }
}

export function userFromToken(token) {
    if (!token) return null;
    const parts = token.split(".");
    if (parts.length < 2) return null;
    const p = JSON.parse(b64urlDecode(parts[1] || ""));
    let roles = p.roles || p.authorities || p.scope || [];
    if (typeof roles === "string") roles = roles.split(/[,\s]+/).filter(Boolean);
    if (!Array.isArray(roles)) roles = [];
    return {
        id: p.id ?? p.userId ?? null,
        username: p.username ?? p.user_name ?? p.preferred_username ?? p.sub ?? null,
        email: p.email ?? null,
        roles: Array.from(new Set(roles.map(String))),
        exp: typeof p.exp === "number" ? p.exp : null,
    };
}

// authenticated fetch helper
export async function authJson(path, init = {}) {
    const headers = new Headers(init.headers || {});
    headers.set("Accept", "application/json");
    const isForm = init.body instanceof FormData;
    if (!isForm && init.body && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }
    const t = getToken();
    if (t) headers.set("Authorization", `Bearer ${t}`);

    const res = await fetch(url(path), { ...init, headers });
    if (!res.ok) {
        const text = await res.text().catch(() => "");
        throw new Error(`HTTP ${res.status} ${text}`);
    }
    const ct = res.headers.get("content-type") || "";
    return ct.includes("application/json") ? res.json() : res.text();
}

// auth endpoints
export async function loginUser(identifier, password) {
    const res = await fetch(url("/auth/login"), {
        method: "POST",
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: JSON.stringify({ username: identifier, password }),
    });
    if (!res.ok) throw new Error(`Login failed: HTTP ${res.status}`);
    const data = await res.json();
    const token = data.accessToken || data.token || data.jwt || data.access_token;
    if (!token) throw new Error("Login failed: token missing");
    return { token, user: data.user || userFromToken(token) || null };
}

export async function registerUser(username, password, role = "user", extra = {}) {
    const res = await fetch(url("/auth/register"), {
        method: "POST",
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: JSON.stringify({ username, password, role, ...extra }),
    });
    if (!res.ok) throw new Error(`Register failed: HTTP ${res.status}`);
    const data = await res.json();
    const token = data.accessToken || data.token || data.jwt || data.access_token || null;
    return { token, user: data.user || (token ? userFromToken(token) : null) };
}
