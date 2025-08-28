// src/components/Header.jsx
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Header() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const linkClass = ({ isActive }) =>
        `nav-link text-dark ${isActive ? 'fw-bold' : ''}`;

    return (
        <nav className="navbar navbar-expand-lg navbar-light bg-light border-bottom">
            <div className="container">
                <NavLink className="navbar-brand fw-bold text-dark" to="/home">
                    SportAdmin
                </NavLink>

                <button className="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbars">
                    <span className="navbar-toggler-icon"></span>
                </button>

                <div className="collapse navbar-collapse" id="navbars">
                    <ul className="navbar-nav ms-auto mb-2 mb-lg-0">
                        {user ? (
                            <>
                                <li className="nav-item">
                                    <NavLink to="/home" className={linkClass} end>Головна</NavLink>
                                </li>
                                <li className="nav-item">
                                    <NavLink to="/dashboard" className={linkClass}>Кабінет</NavLink>
                                </li>
                                <li className="nav-item">
                                    <NavLink to="/events" className={linkClass}>Події</NavLink>
                                </li>
                                <li className="nav-item">
                                    <button
                                        className="btn btn-outline-secondary ms-lg-3"
                                        onClick={async () => { await logout(); navigate('/login'); }}
                                    >
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
