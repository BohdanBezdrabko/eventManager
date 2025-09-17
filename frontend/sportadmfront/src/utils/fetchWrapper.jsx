// src/utils/fetchWrapper.jsx
const API = import.meta.env.VITE_API_URL || '';

function buildUrl(path) {
    // дозволяє як абсолютні, так і відносні шляхи
    if (/^https?:\/\//i.test(path)) return path;
    return `${API}${path}`;
}

async function parseBody(res) {
    const text = await res.text();
    if (!text) return null;
    try { return JSON.parse(text); } catch { return text; }
}

export async function request(path, init = {}) {
    const token = localStorage.getItem('token');

    const isFormData = init.body instanceof FormData;
    const headers = {
        ...(init.headers || {}),
        ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
    };

    const res = await fetch(buildUrl(path), { ...init, headers });

    if (res.status === 401) {
        localStorage.removeItem('token');
        // редірект саме на /login
        window.location.assign('/login');
        throw Object.assign(new Error('Unauthorized'), { status: 401 });
    }

    const data = await parseBody(res);

    if (!res.ok) {
        const message =
            (data && typeof data === 'object' && data.message) ||
            (typeof data === 'string' && data) ||
            `HTTP ${res.status}`;
        throw Object.assign(new Error(message), { status: res.status, data });
    }

    // 204 No Content або порожня відповідь
    return data;
}

// зручні шорткати
export const http = {
    get: (p) => request(p, { method: 'GET' }),
    post: (p, body) =>
        request(p, { method: 'POST', body: body instanceof FormData ? body : JSON.stringify(body) }),
    put: (p, body) =>
        request(p, { method: 'PUT', body: body instanceof FormData ? body : JSON.stringify(body) }),
    patch: (p, body) =>
        request(p, { method: 'PATCH', body: body instanceof FormData ? body : JSON.stringify(body) }),
    delete: (p) => request(p, { method: 'DELETE' }),
};
