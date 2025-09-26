import { http } from "@/utils/fetchWrapper.jsx";

export const getMyRegistrations = () => http.get("/registrations/my");
export const registerForEvent = (eventId) =>
    http.post(`/registrations/my/${eventId}`);
export const cancelRegistration = (eventId) =>
    http.delete(`/registrations/my/${eventId}`);
