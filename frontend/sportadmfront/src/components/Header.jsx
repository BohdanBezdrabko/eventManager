// src/components/Header.jsx
import { NavLink, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext.jsx';

export default function Header() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const linkClass = ({ isActive }) => `nav-link${isActive ? ' active fw-semibold' : ''}`;

    const handleLogout = async () => {
        try { await logout(); } finally { navigate('/login'); }
    };

    return (
        <nav className="navbar navbar-expand-lg navbar-light bg-light border-bottom">
            <div className="container">
                <Link className="navbar-brand fw-bold" to="/">SportAdmin</Link>

                <button
                    className="navbar-toggler"
                    type="button"
                    data-bs-toggle="collapse"
                    data-bs-target="#primaryNavbar"
                    aria-controls="primaryNavbar"
                    aria-expanded="false"
                    aria-label="Toggle navigation"
                >
                    <span className="navbar-toggler-icon" />
                </button>

                <div className="collapse navbar-collapse" id="primaryNavbar">
                    <ul className="navbar-nav ms-auto mb-2 mb-lg-0">
                        {/* публічні лінки */}
                        <li className="nav-item">
                            <NavLink to="/" end className={linkClass}>Головна</NavLink>
                        </li>
                        <li className="nav-item">
                            <NavLink to="/events" className={linkClass}>Події</NavLink>
                        </li>

                        {user ? (
                            <>
                                <li className="nav-item">
                                    <NavLink to="/dashboard" className={linkClass}>Кабінет</NavLink>
                                </li>
                                <li className="nav-item">
                                    <button className="btn btn-outline-secondary ms-lg-3" onClick={handleLogout}>
                                        Вийти
                                    </button>
                                </li>
                            </>
                        ) : (
                            <>
                                <li className="nav-item">
                                    <NavLink to="/login" className={linkClass}>Увійти</NavLink>
                                </li>
                                <li className="nav-item">
                                    <NavLink to="/register" className={linkClass}>Зареєструватися</NavLink>
                                </li>
                            </>
                        )}
                    </ul>
                </div>
            </div>
        </nav>
    );
}
