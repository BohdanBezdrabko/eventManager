// src/components/RouteGuards.jsx
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export function PrivateRoute({ children }) {
    const { user, booting } = useAuth();
    const location = useLocation();
    if (booting) return null;
    if (!user) {
        const from = encodeURIComponent(location.pathname + location.search);
        return <Navigate to={`/login?from=${from}`} replace />;
    }
    return children;
}

export function PublicOnlyRoute({ children }) {
    const { user, booting } = useAuth();
    if (booting) return null;
    return user ? <Navigate to="/dashboard" replace /> : children;
}
export function AdminRoute({ children }) {
    const { user, booting } = useAuth();
    if (booting) return null;
    const roles = Array.isArray(user?.roles)
        ? user.roles
        : typeof user?.roles === "string"
            ? user.roles.split(/[,\s]+/).filter(Boolean)
            : [];
    const isAdmin = roles.includes("ROLE_ADMIN") || roles.includes("ADMIN");
    return user && isAdmin ? children : <Navigate to="/login" replace />;
}
