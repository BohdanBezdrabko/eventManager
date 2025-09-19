// src/services/events.js
import { http } from "@/utils/fetchWrapper";

const BASE = "/events";

/** Список івентів з опціональними параметрами (page, size, q, etc.) */
export const getAllEvents = (params = {}) => {
    const qs = new URLSearchParams(params).toString();
    return http.get(`${BASE}${qs ? `?${qs}` : ""}`);
};

/** Один івент */
export const getEventById = (id) =>
    http.get(`${BASE}/${encodeURIComponent(id)}`);

/** Створення (потрібен JWT) */
export const createEvent = (payload) =>
    http.post(BASE, payload);

/** Оновлення (потрібен JWT) */
export const updateEvent = (id, payload) =>
    http.put(`${BASE}/${encodeURIComponent(id)}`, payload);

/** Видалення (потрібен JWT) */
export const deleteEvent = (id) =>
    http.delete(`${BASE}/${encodeURIComponent(id)}`);
