import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

function Splash() {
    return (
        <div style={{ padding: 24 }}>
            Loading…
        </div>
    );
}

export function PrivateRoute() {
    const { user, booting } = useAuth();
    const location = useLocation();

    if (booting) return <Splash />;

    if (!user) {
        return (
            <Navigate
                to="/login"
                replace
                state={{ from: { pathname: location.pathname, search: location.search, hash: location.hash } }}
            />
        );
    }

    return <Outlet />;
}

export function PublicOnlyRoute() {
    const { user, booting } = useAuth();
    const location = useLocation();

    if (booting) return <Splash />;

    if (user) {
        const from = location.state?.from;
        const redirectTo =
            (from && !(from.pathname === '/login' || from.pathname === '/register'))
                ? `${from.pathname}${from.search || ''}${from.hash || ''}`
                : '/dashboard'; // змінити на '/home', якщо такого маршруту нема

        return <Navigate to={redirectTo} replace />;
    }

    return <Outlet />;
}
