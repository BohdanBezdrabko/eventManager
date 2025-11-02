import { Outlet, useLocation, matchPath } from 'react-router-dom';
import { useMemo } from 'react';
import Header from '@/components/Header';
import Footer from '@/components/Footer.jsx';


const HIDE_HEADER_PATTERNS = [
    '/login',
    '/register',
    '/auth/*',
];

export default function AppLayout() {
    const { pathname } = useLocation();

    const hideHeader = useMemo(
        () => HIDE_HEADER_PATTERNS.some((p) => !!matchPath(p, pathname)),
        [pathname]
    );

    return (
        <div className="d-flex flex-column min-vh-100">
            {!hideHeader && <Header />}
            <main className="flex-grow-1">
                <Outlet />
            </main>
            <Footer />
        </div>
    );
}
