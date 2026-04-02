// src/services/announcementTemplates.jsx
import { http } from "@/utils/fetchWrapper.jsx";

const base = (eventId) => `/events/${encodeURIComponent(eventId)}/announcement-templates`;

/**
 * Отримати всі шаблони оголошення для івенту
 */
export function listTemplates(eventId) {
    return http.get(base(eventId));
}

/**
 * Отримати один шаблон
 */
export function getTemplate(eventId, templateId) {
    return http.get(`${base(eventId)}/${encodeURIComponent(templateId)}`);
}

/**
 * Отримати preview відрендеренного шаблону з реальними даними івенту
 */
export function previewTemplate(eventId, templateId) {
    return http.get(`${base(eventId)}/${encodeURIComponent(templateId)}/preview`);
}

/**
 * Створити новий шаблон оголошення
 */
export function createTemplate(eventId, data) {
    return http.post(base(eventId), data);
}

/**
 * Оновити існуючий шаблон
 */
export function updateTemplate(eventId, templateId, data) {
    return http.put(`${base(eventId)}/${encodeURIComponent(templateId)}`, data);
}

/**
 * Видалити шаблон
 */
export function deleteTemplate(eventId, templateId) {
    return http.delete(`${base(eventId)}/${encodeURIComponent(templateId)}`);
}

/**
 * Рендерити текст оголошення з переданим шаблоном
 * (для тимчасового рендерингу без збереження)
 */
export function renderAnnouncementText(eventId, templateBody) {
    return http.post(`/events/${encodeURIComponent(eventId)}/template-preview/announcement`, {
        templateBody
    });
}


