// src/services/events.jsx
import { http } from "@/utils/fetchWrapper.jsx";

/**
 * УВАГА: у проекті, судячи з логів, базовий префікс /api/v1 додається у fetchWrapper.
 * Тому тут ми використовуємо шляхи без /api/v1: лише "/events" тощо.
 */
const base = "/events";

/** Отримати подію за id */
export const getEventById = (id) => http.get(`${base}/${encodeURIComponent(id)}`);

/**
 * Отримати УСІ події без фільтрів.
 * GET /api/v1/events — список
 * Повертає масив подій (нормалізує різні формати відповіді).
 */
export const getAllEvents = async () => {
    const data = await http.get(base);
    const arr = Array.isArray(data) ? data : (data?.content ?? data?.items ?? data?.data ?? []);
    return Array.isArray(arr) ? arr : [];
};

/** Створити подію — потрібен для CreateEventPage.jsx */
export const createEvent = (body) => http.post(base, body);

export const updateEvent = (id, payload) =>
    http.put(`/events/${encodeURIComponent(id)}`, payload);
/** Видалити подію */
export const deleteEvent = (id) => http.delete(`${base}/${encodeURIComponent(id)}`);

/** Автор події */
export const getEventCreator = (id) =>
    http.get(`${base}/${encodeURIComponent(id)}/creator`);

/** Події автора (якщо десь потрібно) */
export const getEventsByAuthor = ({ userId, page = 0, size = 20, sort = "startAt,desc" }) => {
    if (userId === undefined || userId === null) {
        throw new Error("getEventsByAuthor: userId is required");
    }
    const qs = new URLSearchParams({ page: String(page), size: String(size), sort });
    return http.get(`${base}/by-author/${encodeURIComponent(userId)}?${qs.toString()}`);
};

/** Пошук подій за назвою */
export const getEventsByName = (name) =>
    http.get(`${base}/by-name/${encodeURIComponent(name)}`);

/** Пошук подій за локацією */
export const getEventsByLocation = (location) =>
    http.get(`${base}/by-location/${encodeURIComponent(location)}`);

/** Кількість Telegram-підписок для події (використовується на деталях) */
export const getTelegramSubscriptionCount = (eventId) =>
    http.get(`${base}/${encodeURIComponent(eventId)}/subscription/telegram/count`);

/** Мій статус підписки на подію (якщо використовується у деталях) */
export const getMySubscriptionStatus = (eventId) =>
    http.get(`${base}/${encodeURIComponent(eventId)}/subscription/my-status`);
