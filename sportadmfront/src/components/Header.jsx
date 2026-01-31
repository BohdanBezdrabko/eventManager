import { NavLink, Link, useNavigate, useLocation } from "react-router-dom";
import { useRef, useEffect, useState } from "react";
import { useAuth } from "@/context/AuthContext";
import "./header.css";

export default function Header() {
    const { user, logout, booting } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    const navRef = useRef(null);
    const togglerRef = useRef(null);

    const [menuOpen, setMenuOpen] = useState(false);
    const [isDarkTheme, setIsDarkTheme] = useState(false);

    useEffect(() => {
        const isDark =
            document.documentElement.classList.contains("dark") ||
            localStorage.getItem("theme") === "dark";
        setIsDarkTheme(isDark);

        const observer = new MutationObserver(() => {
            const dark = document.documentElement.classList.contains("dark");
            setIsDarkTheme(dark);
        });

        observer.observe(document.documentElement, {
            attributes: true,
            attributeFilter: ["class"],
        });

        return () => observer.disconnect();
    }, []);

    const linkClass = ({ isActive }) => "nav-link" + (isActive ? " active" : "");

    const isAdmin = (u) => {
        if (!u) return false;
        const roles = Array.isArray(u.roles)
            ? u.roles
            : typeof u.roles === "string"
                ? u.roles.split(/[,\s]+/).filter(Boolean)
                : [];
        return roles.includes("ROLE_ADMIN") || roles.includes("ADMIN");
    };

    const closeMenu = () => setMenuOpen(false);
    const toggleMenu = () => setMenuOpen((prev) => !prev);

    const onLogout = async () => {
        closeMenu();
        try {
            await logout();
        } finally {
            navigate("/login", { replace: true });
        }
    };

    // ✅ Закривати меню при будь-якій зміні маршруту
    useEffect(() => {
        closeMenu();
    }, [location.pathname]);

    // ✅ Клік “поза” (але не закривати, якщо клік по бургеру)
    useEffect(() => {
        const handleClickOutside = (e) => {
            const menuEl = navRef.current;
            const btnEl = togglerRef.current;

            const clickedMenu = menuEl && menuEl.contains(e.target);
            const clickedButton = btnEl && btnEl.contains(e.target);

            if (!clickedMenu && !clickedButton) closeMenu();
        };

        if (menuOpen) {
            document.addEventListener("mousedown", handleClickOutside);
            return () => document.removeEventListener("mousedown", handleClickOutside);
        }
    }, [menuOpen]);

    return (
        <nav className={`navbar ${isDarkTheme ? "dark-theme" : ""}`}>
            <div className="container-fluid">
                <Link className="navbar-brand" to="/" onClick={closeMenu}>
                    EventSys
                </Link>

                <button
                    ref={togglerRef}
                    className={`navbar-toggler ${menuOpen ? "is-open" : ""}`}
                    type="button"
                    onClick={toggleMenu}
                    aria-label="Меню"
                    aria-expanded={menuOpen}
                    aria-controls="main-nav"
                >
          <span className="burger" aria-hidden="true">
            <span />
            <span />
            <span />
          </span>
                </button>

                <div
                    id="main-nav"
                    ref={navRef}
                    className={`navbar-menu ${menuOpen ? "show" : ""}`}
                >
                    <ul className="navbar-nav">
                        <li className="nav-item">
                            <NavLink to="/" className={linkClass} end onClick={closeMenu}>
                                Головна
                            </NavLink>
                        </li>

                        <li className="nav-item">
                            <NavLink to="/events" className={linkClass} onClick={closeMenu}>
                                Івенти
                            </NavLink>
                        </li>

                        {!booting && user && (
                            <li className="nav-item">
                                <NavLink
                                    to="/dashboard"
                                    className={linkClass}
                                    onClick={closeMenu}
                                >
                                    Кабінет
                                </NavLink>
                            </li>
                        )}

                        {!booting && user && isAdmin(user) && (
                            <li className="nav-item">
                                <NavLink
                                    to="/events/create"
                                    className={linkClass}
                                    onClick={closeMenu}
                                >
                                    Створити івент
                                </NavLink>
                            </li>
                        )}

                        <li className="nav-item separator">
                            {booting ? null : !user ? (
                                <NavLink
                                    to="/login"
                                    className={linkClass}
                                    onClick={closeMenu}
                                >
                                    Увійти
                                </NavLink>
                            ) : (
                                <button
                                    className="btn btn-outline-danger btn-sm nav-logout-btn"
                                    onClick={onLogout}
                                >
                                    Вийти ({user.username})
                                </button>
                            )}
                        </li>

                        {!user && !booting && (
                            <li className="nav-item">
                                <NavLink
                                    to="/register"
                                    className={linkClass}
                                    onClick={closeMenu}
                                >
                                    Реєстрація
                                </NavLink>
                            </li>
                        )}
                    </ul>
                </div>
            </div>
        </nav>
    );
}
