// src/components/Header.jsx
import { NavLink, Link, useNavigate } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";

export default function Header() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const linkClass = ({ isActive }) => "nav-link" + (isActive ? " active" : "");

    const onLogout = async () => {
        try {
            await logout();
        } finally {
            navigate("/login", { replace: true });
        }
    };

    return (
        <nav className="navbar navbar-expand-lg navbar-light bg-light shadow-sm">
            <div className="container">
                <Link className="navbar-brand" to="/">EventSys</Link>
                <button className="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#mainNav">
                    <span className="navbar-toggler-icon"></span>
                </button>

                <div className="collapse navbar-collapse" id="mainNav">
                    <ul className="navbar-nav me-auto mb-2 mb-lg-0">
                        <li className="nav-item">
                            <NavLink to="/" className={linkClass} end>Головна</NavLink>
                        </li>
                        <li className="nav-item">
                            <NavLink to="/events" className={linkClass}>Івенти</NavLink>
                        </li>
                        {user && (
                            <li className="nav-item">
                                <NavLink to="/dashboard" className={linkClass}>Кабінет</NavLink>
                            </li>
                        )}
                    </ul>

                    <ul className="navbar-nav ms-auto">
                        {!user ? (
                            <>
                                <li className="nav-item"><NavLink to="/login" className={linkClass}>Увійти</NavLink></li>
                                <li className="nav-item"><NavLink to="/register" className={linkClass}>Реєстрація</NavLink></li>
                            </>
                        ) : (
                            <li className="nav-item">
                                <button className="btn btn-outline-danger" onClick={onLogout}>Вийти</button>
                            </li>
                        )}
                    </ul>
                </div>
            </div>
        </nav>
    );
}
