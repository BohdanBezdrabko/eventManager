// src/components/RouteGuards.jsx
import { Navigate } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";

export function PrivateRoute({ children }) {
    const { user, booting } = useAuth();
    if (booting) return null; // або лоадер
    return user ? children : <Navigate to="/login" replace />;
}

export function PublicOnlyRoute({ children }) {
    const { user, booting } = useAuth();
    if (booting) return null;
    return user ? <Navigate to="/dashboard" replace /> : children;
}
