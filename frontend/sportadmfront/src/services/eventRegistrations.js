// src/services/eventRegistrations.js
import { http } from "@/utils/fetchWrapper.jsx";
import { authJson } from "@/services/auth.jsx";

async function resolveUserId(user) {
    if (user?.id) return user.id;
    const me = await authJson("/auth/me");
    if (!me?.id) throw new Error("No user id");
    return me.id;
}

export async function getUserEvents(user) {
    const id = await resolveUserId(user);
    return http.get(`/registrations/user-id/${id}`);
}
