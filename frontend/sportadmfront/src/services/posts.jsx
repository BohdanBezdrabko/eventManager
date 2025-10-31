// src/services/posts.jsx
import { http } from "@/utils/fetchWrapper.jsx";

/** Статуси поста */
export const PostStatus = {
    DRAFT: "DRAFT",
    SCHEDULED: "SCHEDULED",
    PUBLISHED: "PUBLISHED",
    FAILED: "FAILED",
    CANCELLED: "CANCELLED",
};

/** Аудиторія */
export const Audience = {
    PUBLIC: "PUBLIC",
    SUBSCRIBERS: "SUBSCRIBERS",
};

/** Канал публікації */
export const Channel = {
    TELEGRAM: "TELEGRAM",
    EMAIL: "EMAIL",
};

const base = (eventId) => `/events/${encodeURIComponent(eventId)}/posts`;

/** Список постів (фільтри: status, audience, channel) */
export function listPosts(eventId, { status, audience, channel, short } = {}) {
    const params = new URLSearchParams();
    if (status) params.set("status", status);
    if (audience) params.set("audience", audience);
    if (channel) params.set("channel", channel);
    if (short) params.set("short", "true");
    const qs = params.toString();
    return http.get(qs ? `${base(eventId)}?${qs}` : base(eventId));
}

/** Отримати пост */
export function getPost(eventId, postId) {
    return http.get(`${base(eventId)}/${encodeURIComponent(postId)}`);
}

/** Створити пост */
export function createPost(eventId, payload) {
    return http.post(base(eventId), payload);
}

/** Оновити пост */
export function updatePost(eventId, postId, payload) {
    return http.put(`${base(eventId)}/${encodeURIComponent(postId)}`, payload);
}

/** Видалити пост */
export function deletePost(eventId, postId) {
    return http.delete(`${base(eventId)}/${encodeURIComponent(postId)}`);
}

/** Змінити статус (PATCH /status) */
export function setPostStatus(eventId, postId, { status, error }) {
    return http.patch(`${base(eventId)}/${encodeURIComponent(postId)}/status`, { status, error });
}

/** Опублікувати негайно */
export function publishNow(eventId, postId) {
    return http.post(`${base(eventId)}/${encodeURIComponent(postId)}/publish-now`, {});
}
