// src/services/eventRegistrations.js
import { http } from "@/utils/fetchWrapper";

const BASE = "/api/v1/registrations";

export const getUserEvents = (userId) => http.get(`${BASE}/user/${encodeURIComponent(userId)}`);
export const registerForEvent = (eventId) => http.post(`${BASE}/event/${encodeURIComponent(eventId)}`, {});
export const cancelRegistration = (eventId) => http.delete(`${BASE}/event/${encodeURIComponent(eventId)}`);
