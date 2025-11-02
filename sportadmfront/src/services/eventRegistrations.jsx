import { http } from "@/utils/fetchWrapper.jsx";

const base = "/registrations";

// Список моїх реєстрацій (DTO: id, userId, username, eventId, eventTitle, registrationDate)
export const getMyRegistrations = () => http.get(`${base}/my`);

// Зареєструватися на подію
export const registerForEvent = (eventId) => http.post(`${base}/my/${eventId}`, {});

// Скасувати реєстрацію
export const cancelRegistration = (eventId) => http.delete(`${base}/my/${eventId}`);
