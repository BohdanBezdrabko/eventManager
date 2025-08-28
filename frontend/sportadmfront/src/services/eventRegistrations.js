import { getToken } from "./auth.jsx";

const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8081";

export async function getUserEvents(userId) {
    const res = await fetch(`${API_URL}/registration/user/${userId}`, {
        headers: {
            "Authorization": `Bearer ${getToken()}`,
            "Content-Type": "application/json",
        },
    });

    if (!res.ok) {
        throw new Error("Не вдалося завантажити івенти користувача");
    }
    return res.json();
}
