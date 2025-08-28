// src/services/auth.js
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8081';

// ===== Token storage =====
function setToken(token) {
    if (token) localStorage.setItem('token', token);
}
function getToken() {
    return localStorage.getItem('token');
}
function clearToken() {
    localStorage.removeItem('token');
}

export { setToken, getToken, clearToken };

async function readError(res) {
    const txt = await res.text();
    try {
        const parsed = JSON.parse(txt);
        return parsed.message || txt || res.statusText;
    } catch {
        return txt || res.statusText;
    }
}

// ===== Auth API =====

/**
 * Реєстрація: очікуємо { token, user } від бекенду.
 * Повертаємо { token, user } і зберігаємо токен у localStorage.
 */
export async function registerUser({ username, password, role = 'participant' }) {
    const res = await fetch(`${API_URL}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password, role }),
    });

    if (!res.ok) {
        throw new Error((await readError(res)) || 'Registration failed');
    }

    const data = await res.json(); // { token, user }
    if (!data?.token || !data?.user) {
        throw new Error('Invalid response from server');
    }

    setToken(data.token);
    return { token: data.token, user: data.user };
}

/**
 * Логін: очікуємо { token, user }.
 */
export async function loginUser({ username, password }) {
    const res = await fetch(`${API_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
    });

    if (!res.ok) {
        throw new Error((await readError(res)) || 'Login failed');
    }

    const data = await res.json(); // { token, user }
    if (!data?.token || !data?.user) {
        throw new Error('Invalid response from server');
    }

    setToken(data.token);
    return { token: data.token, user: data.user };
}

/**
 * Отримати поточного користувача з бекенду.
 */
export async function getCurrentUser() {
    const token = getToken();
    if (!token) throw new Error('No token');

    const res = await fetch(`${API_URL}/auth/user/me`, {
        method: 'GET',
        headers: { Authorization: `Bearer ${token}` },
    });

    if (res.status === 401) {
        clearToken();
        throw new Error('Unauthorized');
    }

    if (!res.ok) {
        throw new Error((await readError(res)) || 'Не вдалося отримати дані користувача');
    }

    return res.json();
}

/** Логаут: просто чистимо токен (JWT — stateless) */
export function logoutUser() {
    clearToken();
}

// Хелпер для додавання Authorization у ваші інші fetch-и:
export function authHeaders(extra = {}) {
    const token = getToken();
    return token ? { ...extra, Authorization: `Bearer ${token}` } : extra;
}

// Універсальний fetch з токеном та базовою обробкою
export async function authFetch(path, options = {}) {
    const url = path.startsWith('http') ? path : `${API_URL}${path}`;
    const res = await fetch(url, {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            ...authHeaders(options.headers),
        },
    });

    if (res.status === 401) {
        clearToken();
        throw new Error('Unauthorized');
    }

    if (res.status === 204) return null;

    const text = await res.text();
    if (!text) return null;
    try {
        return JSON.parse(text);
    } catch {
        return text;
    }
}
