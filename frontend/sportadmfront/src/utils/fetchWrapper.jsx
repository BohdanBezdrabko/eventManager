// src/utils/fetchWrapper.jsx
import { getToken, clearToken } from "@/services/auth";

const API_ROOT = import.meta.env.VITE_API_URL || "";

/** Build absolute URL from relative API path */
function buildUrl(path) {
    if (/^https?:\/\//i.test(path)) return path;
    return `${API_ROOT}${path}`;
}

/** Universal request helper with JSON defaults and auth */
export async function request(path, init = {}) {
    const url = buildUrl(path);

    const headers = new Headers(init.headers || {});
    if (!headers.has("Accept")) headers.set("Accept", "application/json");
    const isFormData = init.body instanceof FormData;
    if (!isFormData && init.body && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }

    const token = getToken();
    if (token && !headers.has("Authorization")) {
        headers.set("Authorization", `Bearer ${token}`);
    }

    const res = await fetch(url, { ...init, headers });

    if (res.status === 401) {
        clearToken();
        throw new Error("Unauthorized");
    }

    if (res.status === 204) return null;

    const text = await res.text();
    if (!text) return null;
    try { return JSON.parse(text); } catch { return text; }
}

/** Verb helpers */
export const http = {
    get: (p) => request(p, { method: "GET" }),
    post: (p, body) => request(p, { method: "POST", body: body instanceof FormData ? body : JSON.stringify(body) }),
    put: (p, body) => request(p, { method: "PUT", body: body instanceof FormData ? body : JSON.stringify(body) }),
    patch: (p, body) => request(p, { method: "PATCH", body: body instanceof FormData ? body : JSON.stringify(body) }),
    delete: (p) => request(p, { method: "DELETE" }),
};
