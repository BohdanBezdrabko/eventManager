// src/layouts/AppLayout.jsx
import { Outlet, useLocation } from 'react-router-dom';
import Header from '../components/Header';
import { useAuth } from '../context/AuthContext';

export default function AppLayout() {
    const { booting } = useAuth();
    const { pathname } = useLocation();

    // Шляхи без хедера
    const hideHeader = pathname.startsWith('/login') || pathname.startsWith('/register');

    if (booting) {
        return (
            <div className="container-fluid vh-100 d-flex justify-content-center align-items-center">
                <div className="spinner-border" role="status">
                    <span className="visually-hidden">Завантаження...</span>
                </div>
            </div>
        );
    }

    return (
        <>
            {!hideHeader && <Header />}
            <Outlet />
        </>
    );
}
