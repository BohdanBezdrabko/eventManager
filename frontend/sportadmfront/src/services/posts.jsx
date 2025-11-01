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

/** Канал */
export const Channel = {
    TELEGRAM: "TELEGRAM",
    INSTAGRAM: "INSTAGRAM",
    FACEBOOK: "FACEBOOK",
};

/** Аудиторія */
export const Audience = {
    PUBLIC: "PUBLIC",
    SUBSCRIBERS: "SUBSCRIBERS",
};

const base = (eventId) => `/events/${encodeURIComponent(eventId)}/posts`;

export function listPosts(eventId, { page, size, status, audience, channel, short } = {}) {
    const params = new URLSearchParams();
    if (page !== undefined) params.set("page", String(page));
    if (size !== undefined) params.set("size", String(size));
    if (status) params.set("status", status);
    if (audience) params.set("audience", audience);
    if (channel) params.set("channel", channel);
    if (short) params.set("short", "true");
    const qs = params.toString();
    return http.get(qs ? `${base(eventId)}?${qs}` : base(eventId));
}

/** КОРОТКИЙ список постів події: GET /api/v1/events/{eventId}/posts/short */
export async function listPostsShort(eventId) {
    // Якщо сюди випадково прийде undefined — відразу повернемо порожній масив,
    // щоб НЕ робити запит у /events/undefined/...
    if (eventId === undefined || eventId === null) return [];
    try {
        const res = await http.get(`${base(eventId)}/short`);
        // нормалізуємо відповідь до масиву
        return Array.isArray(res) ? res : (res?.content ?? res?.items ?? res?.data ?? []);
    } catch (e) {
        // Якщо бек віддає 404 для подій без постів — теж повертаємо пустий масив
        if (e?.status === 404) return [];
        throw e;
    }
}

export function getPost(eventId, postId) {
    return http.get(`${base(eventId)}/${encodeURIComponent(postId)}`);
}

export function createPost(eventId, body) {
    return http.post(base(eventId), body);
}

export function updatePost(eventId, postId, body) {
    return http.put(`${base(eventId)}/${encodeURIComponent(postId)}`, body);
}

export function deletePost(eventId, postId) {
    return http.delete(`${base(eventId)}/${encodeURIComponent(postId)}`);
}

export function setPostStatus(eventId, postId, { status, error }) {
    return http.patch(`${base(eventId)}/${encodeURIComponent(postId)}/status`, { status, error });
}

export function publishNow(eventId, postId) {
    return http.post(`${base(eventId)}/${encodeURIComponent(postId)}/publish-now`, {});
}
