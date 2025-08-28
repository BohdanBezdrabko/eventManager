import { useLocation } from 'react-router-dom';
import Header from './Header.jsx';

const HIDE_HEADER_PATHS = ['/', '/landing', 'register', '/login']; // тут можна додати інші шляхи без хедера

export default function Layout({ children }) {
    const location = useLocation();
    const showHeader = !HIDE_HEADER_PATHS.includes(location.pathname);

    return (
        <div className="d-flex flex-column min-vh-100">
            {showHeader && <Header />}
            <main className="flex-fill">{children}</main>
            <footer className="text-center py-3 text-muted small border-top">
                © {new Date().getFullYear()} SportAdmin
            </footer>
        </div>
    );
}
