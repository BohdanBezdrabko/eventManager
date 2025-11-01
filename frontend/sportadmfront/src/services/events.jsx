// src/services/events.jsx
import { authJson } from "@/services/auth.jsx";

const base = "/events";

/** Подія за id */
export const getEventById = (id) => authJson(`${base}/${encodeURIComponent(id)}`);

/** Умовний лістинг (на майбутнє; бек може мати власний /events?...) */
export const getAllEvents = (params) => {
    const qs = params
        ? "?" +
        Object.entries(params)
            .filter(([, v]) => v !== undefined && v !== null && v !== "")
            .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
            .join("&")
        : "";
    return authJson(`${base}${qs}`);
};

/** Пошук */
export const searchEventsByName = (name) =>
    authJson(`${base}/by-name/${encodeURIComponent(name)}`);

export const searchEventsByLocation = (location) =>
    authJson(`${base}/by-location/${encodeURIComponent(location)}`);

/** Автор події */
export const getEventCreator = (id) =>
    authJson(`${base}/${encodeURIComponent(id)}/creator`);

/** Дерево івенту + короткі пости */
export const getEventTree = (eventId) =>
    authJson(`${base}/${encodeURIComponent(eventId)}/tree`);

/** Створити / Оновити / Видалити */
export const createEvent = (payload) => authJson(`${base}`, {
    method: "POST",
    body: JSON.stringify(payload),
    headers: { "Content-Type": "application/json" },
});

export const updateEvent = (id, payload) => authJson(`${base}/${encodeURIComponent(id)}`, {
    method: "PUT",
    body: JSON.stringify(payload),
    headers: { "Content-Type": "application/json" },
});

export const deleteEvent = (id) => authJson(`${base}/${encodeURIComponent(id)}`, {
    method: "DELETE",
});

/** !!! ГОЛОВНИЙ ФІКС !!!
 * Раніше тут був запит на /events?createdBy=...
 * Правильно: GET /events/by-author/{userId}?page=&size=&sort=
 */
export const getEventsByAuthor = ({ userId, page = 0, size = 20, sort = "startAt,desc" }) => {
    if (userId === undefined || userId === null) {
        throw new Error("getEventsByAuthor: userId is required");
    }
    const qs = new URLSearchParams({ page: String(page), size: String(size), sort });
    return authJson(`${base}/by-author/${encodeURIComponent(userId)}?${qs.toString()}`);
};

/** Підписки / Telegram: кількість підписаних */
export const getTelegramSubscriptionCount = (eventId) =>
    authJson(`${base}/${encodeURIComponent(eventId)}/subscription/telegram/count`);
