// Універсальний витягач ідентифікатора користувача з AuthContext або токена
function base64UrlToJson(b64) {
    try {
        const norm = b64.replace(/-/g, "+").replace(/_/g, "/");
        const json = decodeURIComponent(
            atob(norm)
                .split("")
                .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
                .join("")
        );
        return JSON.parse(json);
    } catch {
        return null;
    }
}

export function decodeJwtPayload(token) {
    if (!token) return null;
    const parts = token.split(".");
    if (parts.length < 2) return null;
    return base64UrlToJson(parts[1]);
}

export function getUserIdFromAuth(user) {
    // 1) Спробувати напряму з об’єкта user (різні провайдери кладуть по-різному)
    const fromUser =
        user?.id ??
        user?.userId ??
        user?.user?.id ??
        user?.user?.userId ??
        user?.sub ??
        user?.subject ??
        user?.uid ??
        user?.user_id ??
        user?.nameid ??
        null;

    if (fromUser != null) return String(fromUser);

    // 2) Спробувати з токена (access_token / token)
    const token =
        localStorage.getItem("access_token") ||
        localStorage.getItem("token") ||
        sessionStorage.getItem("access_token") ||
        sessionStorage.getItem("token");

    const payload = decodeJwtPayload(token);
    const fromJwt =
        payload?.id ??
        payload?.userId ??
        payload?.sub ??
        payload?.uid ??
        payload?.user_id ??
        payload?.nameid ??
        null;

    return fromJwt != null ? String(fromJwt) : null;
}
