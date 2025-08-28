import { useAuth } from "../context/AuthContext";
import { useEffect, useState } from "react";
import { getUserEvents } from "../services/eventRegistrations.js";

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

    return (
        <div className="container py-5">
            <div className="card shadow-lg">
                <div className="card-body">
                    <h3 className="card-title mb-3">Кабінет</h3>

                    {user ? (
                        <>
                            <p className="mb-2">
                                <strong>Логін:</strong> {user.username}
                            </p>
                            <p className="mb-2">
                                <strong>Ролі:</strong>{" "}
                                {user.roles && user.roles.length > 0
                                    ? user.roles.join(", ")
                                    : "—"}
                            </p>

                            <h5 className="mt-4">Мої івенти</h5>
                            {loading ? (
                                <p>Завантаження...</p>
                            ) : events.length > 0 ? (
                                <ul className="list-group">
                                    {events.map((ev) => (
                                        <li key={ev.id} className="list-group-item">
                                            <strong>{ev.name}</strong> — {ev.location} (
                                            {ev.date})
                                        </li>
                                    ))}
                                </ul>
                            ) : (
                                <div className="alert alert-secondary mt-2 mb-0">
                                    У вас немає зареєстрованих івентів
                                </div>
                            )}
                        </>
                    ) : (
                        <div className="alert alert-warning mb-0">
                            Дані користувача не завантажені
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
