import { useAuth } from "../context/AuthContext";
import { useEffect, useState } from "react";
import { getUserEvents } from "../services/eventRegistrations.js";
import { Calendar, MapPin, User, Shield } from "lucide-react";

export default function DashboardPage() {
    const { user } = useAuth();
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchEvents = async () => {
            if (user?.id) {
                try {
                    const data = await getUserEvents(user.id);
                    setEvents(data);
                } catch (err) {
                    console.error("Помилка завантаження івентів:", err);
                } finally {
                    setLoading(false);
                }
            } else {
                setLoading(false);
            }
        };

        fetchEvents();
    }, [user]);

    const mapRole = (role) => {
        if (!role) return "—";
        switch (role.toUpperCase()) {
            case "ROLE_ADMIN":
                return "Організатор";
            case "ROLE_USER":
                return "Учасник";
            default:
                return role;
        }
    };

    return (
        <div className="container py-5">
            <h1 className="fw-bold mb-5 text-center text-primary">
                <User size={32} className="me-2" />
                Мій кабінет
            </h1>

            {user ? (
                <>
                    {/* Блок з інформацією про користувача */}
                    <div className="row g-4 mb-5">
                        <div className="col-md-6">
                            <div className="card shadow-sm border-0 rounded-4 h-100">
                                <div className="card-body p-4">
                                    <h5 className="fw-semibold text-secondary mb-3">
                                        <User size={20} className="me-2" />
                                        Дані користувача
                                    </h5>
                                    <p className="fs-5 mb-2">
                                        <span className="fw-semibold">Логін:</span>{" "}
                                        {user.username}
                                    </p>
                                    <p className="fs-5 mb-0">
                                        <span className="fw-semibold">Ролі:</span>{" "}
                                        {user.roles && user.roles.length > 0 ? (
                                            user.roles.map((role, idx) => (
                                                <span
                                                    key={idx}
                                                    className="badge bg-primary-subtle text-primary-emphasis rounded-pill me-2 px-3 py-2"
                                                    style={{ fontSize: "1rem" }}
                                                >
                                                    {mapRole(role)}
                                                </span>
                                            ))
                                        ) : (
                                            "—"
                                        )}
                                    </p>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Блок з івентами */}
                    <h3 className="fw-bold mb-4 text-success d-flex align-items-center">
                        <Calendar className="me-2" size={24} /> Мої івенти
                    </h3>

                    {loading ? (
                        <div className="text-center py-5">
                            <div
                                className="spinner-border text-success"
                                style={{ width: "3rem", height: "3rem" }}
                                role="status"
                            ></div>
                            <p className="mt-3 text-muted fs-5">Завантаження...</p>
                        </div>
                    ) : events.length > 0 ? (
                        <div className="row g-4">
                            {events.map((ev) => (
                                <div key={ev.id} className="col-md-6 col-lg-4">
                                    <div className="card shadow-sm border-0 rounded-4 h-100">
                                        <div className="card-body p-4 d-flex flex-column">
                                            <h5 className="fw-bold mb-2">{ev.name}</h5>
                                            <p className="text-muted mb-3 d-flex align-items-center">
                                                <MapPin size={16} className="me-1" /> {ev.location}
                                            </p>
                                            <p className="small text-secondary mb-4">
                                                Дата: {ev.date}
                                            </p>
                                            <div className="mt-auto">
                                                <span className="badge bg-success px-3 py-2">
                                                    Зареєстровано
                                                </span>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className="alert alert-secondary fs-5">
                            У вас немає зареєстрованих івентів
                        </div>
                    )}
                </>
            ) : (
                <div className="alert alert-warning fs-5 mb-0">
                    Дані користувача не завантажені
                </div>
            )}
        </div>
    );
}
