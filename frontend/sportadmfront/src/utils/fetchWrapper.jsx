// src/utils/fetchWrapper.jsx
import { getToken, clearToken } from "@/services/auth.jsx";

const API_ROOT = (import.meta.env.VITE_API_URL || "http://localhost:8081/api/v1").replace(/\/+$/, "");

function apiUrl(path) {
    return /^https?:\/\//i.test(path) ? path : `${API_ROOT}${path.startsWith("/") ? path : `/${path}`}`;
}

export async function request(path, init = {}) {
    const headers = new Headers(init.headers || {});
    const isForm = init.body instanceof FormData;

    headers.set("Accept", "application/json");
    if (!isForm && init.body && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }

    const t = getToken();
    if (t) headers.set("Authorization", `Bearer ${t}`);

    const res = await fetch(apiUrl(path), { ...init, headers });

    if (res.status === 401) {
        clearToken();
        throw new Error("Unauthorized 401");
    }

    if (!res.ok) {
        const text = await res.text().catch(() => "");
        throw new Error(`HTTP ${res.status} ${text}`);
    }

    const ct = res.headers.get("content-type") || "";
    return ct.includes("application/json") ? res.json() : res.text();
}

export const http = {
    get: (p) => request(p, { method: "GET" }),
    post: (p, body) =>
        request(p, { method: "POST", body: body instanceof FormData ? body : JSON.stringify(body ?? {}) }),
    put: (p, body) =>
        request(p, { method: "PUT", body: body instanceof FormData ? body : JSON.stringify(body ?? {}) }),
    patch: (p, body) =>
        request(p, { method: "PATCH", body: body instanceof FormData ? body : JSON.stringify(body ?? {}) }),
    delete: (p) => request(p, { method: "DELETE" }),
};
