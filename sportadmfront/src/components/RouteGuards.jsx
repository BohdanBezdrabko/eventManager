// src/components/RouteGuards.jsx
import { Navigate, useLocation, Outlet } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";

/** Доступ лише для автентифікованих користувачів */
export function PrivateRoute() {
    const { user, booting } = useAuth();
    const location = useLocation();
    if (booting) return null;
    if (!user) {
        const from = encodeURIComponent(location.pathname + location.search);
        return <Navigate to={`/login?from=${from}`} replace />;
    }
    return <Outlet />;
}

/** Доступ лише для НЕавтентифікованих (логін/реєстрація) */
export function PublicOnlyRoute() {
    const { user, booting } = useAuth();
    if (booting) return null;
    return user ? <Navigate to="/dashboard" replace /> : <Outlet />;
}

/** Доступ лише для ADMIN */
export function AdminRoute() {
    const { user, booting } = useAuth();
    if (booting) return null;
    const roles = Array.isArray(user?.roles)
        ? user.roles
        : typeof user?.roles === "string"
            ? user.roles.split(/[\s,]+/).filter(Boolean)
            : [];
    const isAdmin = roles.includes("ROLE_ADMIN") || roles.includes("ADMIN") || roles.includes("SUPER_ADMIN");
    return user && isAdmin ? <Outlet /> : <Navigate to="/login" replace />;
}
