export async function authFetch(path, opts = {}) {
    const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8081';
    const token = localStorage.getItem('token');
    const res = await fetch(`${API_URL}${path}`, {
        ...opts,
        headers: {
            'Content-Type': 'application/json',
            ...opts.headers,
            Authorization: token ? `Bearer ${token}` : undefined,
        },
    });
    if (res.status === 401) {
        window.location.href = '/register';
        return;
    }
    return await res.json();
}
