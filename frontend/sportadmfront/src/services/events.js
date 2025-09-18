// src/services/events.js
import { http } from "@/utils/fetchWrapper";

const BASE = "/api/v1/events";

export const getAllEvents = () => http.get(`${BASE}`);
export const getEventById = (id) => http.get(`${BASE}/${encodeURIComponent(id)}`);
export const getEventsByName = (name) => http.get(`${BASE}/name/${encodeURIComponent(name)}`);
export const getEventsByLocation = (location) => http.get(`${BASE}/location/${encodeURIComponent(location)}`);

// Auth-required examples
export const createEvent = (payload) => http.post(`${BASE}`, payload);
export const updateEvent = (id, payload) => http.put(`${BASE}/${encodeURIComponent(id)}`, payload);
export const deleteEvent = (id) => http.delete(`${BASE}/${encodeURIComponent(id)}`);
