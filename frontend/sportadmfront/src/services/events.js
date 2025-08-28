// src/services/events.js
const API_BASE = 'http://localhost:8081';

export async function getAllEvents() {
    const res = await fetch(`${API_BASE}/event/all`, { headers: { Accept: 'application/json' } });
    if (!res.ok) throw new Error('Не вдалося завантажити події');
    return res.json();
}

export async function getEventsByName(name) {
    const res = await fetch(`${API_BASE}/event/name/${encodeURIComponent(name)}`, { headers: { Accept: 'application/json' } });
    if (!res.ok) throw new Error('Не вдалося знайти події за назвою');
    return res.json();
}

export async function getEventsByLocation(location) {
    const res = await fetch(`${API_BASE}/event/location/${encodeURIComponent(location)}`, { headers: { Accept: 'application/json' } });
    if (!res.ok) throw new Error('Не вдалося знайти події за локацією');
    return res.json();
}
