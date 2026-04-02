// src/services/whatsapp.jsx
import { http } from "@/utils/fetchWrapper.jsx";

const base = "/whatsapp";

/**
 * Створити bind code для групи
 * @param groupId - ID групи (формат: 120363xxxxx@g.us)
 * @param groupName - Назва групи
 * @returns {Promise<{code: string, groupId: string, groupName: string, expiresIn: string}>}
 */
export const createBindCode = (groupId, groupName) =>
    http.post(`${base}/groups/bind-codes`, {
        groupId,
        groupName,
    });

/**
 * Підтвердити bind code і прив'язати групу до події
 * @param code - Код (6 цифр)
 * @param eventId - ID події
 * @param isAnnouncement - Чи це announcement група
 * @returns {Promise<{success: boolean, message: string, eventId: number, isAnnouncement: boolean}>}
 */
export const confirmBindCode = (code, eventId, isAnnouncement = false) =>
    http.post(`${base}/groups/confirm-bind`, {
        code,
        eventId,
        isAnnouncement,
    });

/**
 * Отримати текст запрошення та посилання для події
 * @param eventId - ID події
 * @param shortVersion - true для короткої версії (для announcement груп)
 * @returns {Promise<{eventId, eventName, text, waLink, shortVersion}>}
 */
export const getEventInvite = (eventId, shortVersion = false) =>
    http.get(`${base}/events/${encodeURIComponent(eventId)}/invite${shortVersion ? "?short=true" : ""}`);

/**
 * Отримати список прив'язаних груп для події
 * @param eventId - ID події
 * @returns {Promise<{eventId, eventName, groups: Array, total: number}>}
 */
export const getEventGroups = (eventId) =>
    http.get(`${base}/events/${encodeURIComponent(eventId)}/groups`);

/**
 * Видалити прив'язку групи від події
 * @param eventId - ID події
 * @param groupId - ID групи
 * @returns {Promise<{success: boolean, message: string, eventId, groupId}>}
 */
export const removeEventGroup = (eventId, groupId) =>
    http.delete(`${base}/events/${encodeURIComponent(eventId)}/groups/${encodeURIComponent(groupId)}`);
