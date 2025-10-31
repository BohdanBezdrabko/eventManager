// src/services/events.jsx
import { authJson } from "@/services/auth.jsx";

const base = "/events";

export const getEventById = (id) => authJson(`${base}/${id}`);

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

export const createEvent = (payload) =>
    authJson(`${base}`, { method: "POST", body: JSON.stringify(payload) });

export const updateEvent = (id, payload) =>
    authJson(`${base}/${id}`, { method: "PUT", body: JSON.stringify(payload) });

export const deleteEvent = (id) =>
    authJson(`${base}/${id}`, { method: "DELETE" });

/** Кількість телеграм-підписок на івент */
export const getTelegramSubscriptionCount = async (eventId) =>
    authJson(`${base}/${encodeURIComponent(eventId)}/subscription/telegram/count`);

/** Події за автором — лише через ?createdBy= */
export const getEventsByAuthor = async ({
                                            userId,
                                            page = 0,
                                            size = 1000,
                                            sort = "startAt,desc",
                                        }) => {
    const params = new URLSearchParams({
        createdBy: String(userId),
        page: String(page),
        size: String(size),
        sort,
    });
    return authJson(`${base}?${params.toString()}`);
};
