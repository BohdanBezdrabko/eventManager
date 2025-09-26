import { http } from "@/utils/fetchWrapper.jsx";

const base = "/events";

export const getEventById = (id) => http.get(`${base}/${id}`);

export const getAllEvents = (params) => {
    const qs = params
        ? "?" +
        Object.entries(params)
            .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
            .join("&")
        : "";
    return http.get(`${base}${qs}`);
};

// NEW
export const createEvent = (payload) => http.post(`${base}`, payload);
export const updateEvent = (id, payload) => http.put(`${base}/${id}`, payload);
export const deleteEvent = (id) => http.delete(`${base}/${id}`);
