// src/pages/DashboardPage.jsx
import { useAuth } from "@/context/AuthContext";
import { useEffect, useState } from "react";
import { getUserEvents } from "@/services/eventRegistrations";
import { Calendar, MapPin, User, Shield } from "lucide-react";

function normalizeRoles(raw) {
    if (!raw) return [];
    if (Array.isArray(raw)) return raw;
    if (typeof raw === "string") return raw.split(",").map(s => s.trim()).filter(Boolean);
    return [];
}

function prettyRole(code) {
    const map = {
        ROLE_ADMIN: "Адміністратор",
        ROLE_MANAGER: "Менеджер",
        ROLE_USER: "Користувач",
    };
    return map[code] || code;
}

export default function DashboardPage() {
    const { user } = useAuth();
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchEvents = async () => {
            if (user?.id) {
                try {
                    const data = await getUserEvents(user.id);
                    setEvents(Array.isArray(data) ? data : []);
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

    const roles = normalizeRoles(user?.roles);

    return (
        <div className="container py-5">
            <div className="card shadow-lg border-0 rounded-3">
                <div className="card-body p-4">
                    <div className="d-flex align-items-center justify-content-between mb-3">
                        <h2 className="m-0 d-flex align-items-center gap-2">
                            <User size={20} /> Привіт, {user?.username || "користувачу"}
                        </h2>
                        <div className="badge bg-secondary d-flex align-items-center gap-1">
                            <Shield size={16} /> {roles.length ? roles.map(prettyRole).join(", ") : "Без ролей"}
                        </div>
                    </div>

                    <hr className="my-3" />

                    <h5 className="mb-3 d-flex align-items-center gap-2">
                        <Calendar size={18} /> Мої реєстрації
                    </h5>

                    {loading ? (
                        <p className="text-muted">Завантаження…</p>
                    ) : events.length === 0 ? (
                        <p className="text-muted">Поки немає зареєстрованих івентів.</p>
                    ) : (
                        <ul className="list-group">
                            {events.map((e) => (
                                <li key={e.id} className="list-group-item d-flex justify-content-between align-items-center">
                                    <div>
                                        <div className="fw-semibold">{e.name}</div>
                                        <div className="text-muted small d-flex align-items-center gap-2">
                                            <MapPin size={14} /> {e.location || "—"}
                                        </div>
                                    </div>
                                    <span className="badge bg-primary rounded-pill">{e.date || ""}</span>
                                </li>
                            ))}
                        </ul>
                    )}
                </div>
            </div>
        </div>
    );
}
