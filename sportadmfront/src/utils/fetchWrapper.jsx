// src/utils/fetchWrapper.jsx
import { getToken } from "@/services/auth.jsx";

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

    const token = getToken?.();
    if (token && !headers.has("Authorization")) {
        headers.set("Authorization", `Bearer ${token}`);
    }

    const res = await fetch(apiUrl(path), {
        method: init.method || "GET",
        headers,
        body: isForm ? init.body : init.body ?? undefined,
        credentials: "include", // якщо бек користує куки — не завадить
    });

    if (!res.ok) {
        let msg = `HTTP ${res.status}`;
        try {
            const ct = res.headers.get("content-type") || "";
            if (ct.includes("application/json")) {
                const data = await res.json();
                msg = data.message || data.error || JSON.stringify(data);
            } else {
                msg = await res.text();
            }
        } catch {
            // no-op
        }
        throw new Error(msg || `Request failed with ${res.status}`);
    }

    const ct = res.headers.get("content-type") || "";
    return ct.includes("application/json") ? res.json() : res.text();
}

export const http = {
    get: (p) => request(p, { method: "GET" }),
    post: (p, body) =>
        request(p, {
            method: "POST",
            body: body instanceof FormData ? body : JSON.stringify(body ?? {}),
        }),
    put: (p, body) =>
        request(p, {
            method: "PUT",
            body: body instanceof FormData ? body : JSON.stringify(body ?? {}),
        }),
    patch: (p, body) =>
        request(p, {
            method: "PATCH",
            body: body instanceof FormData ? body : JSON.stringify(body ?? {}),
        }),
    delete: (p) => request(p, { method: "DELETE" }),
};
