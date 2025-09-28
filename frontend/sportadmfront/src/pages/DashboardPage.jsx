import {useEffect, useMemo, useState} from "react";
import {Link} from "react-router-dom";
import {getMyRegistrations} from "@/services/eventRegistrations.jsx";
import {getEventById} from "@/services/events.jsx";
import {useAuth} from "@/context/AuthContext";

function byStartAsc(a, b) {
    const ta = new Date(a.startAt || 0).getTime() || 0;
    const tb = new Date(b.startAt || 0).getTime() || 0;
    return ta - tb;
}

function EventItem({ev}) {
    const start = ev.startAt || "";
    const capacity = Number(ev.capacity || 0) || "∞";
    const registered = Number(ev.registeredCount || 0) || 0;
    const free = capacity === "∞" ? "∞" : Math.max(capacity - registered, 0);
    return (
        <div className="card card--row">
            <div className="card__main">
                <h4 className="card__title mb-1">
                    <Link to={`/events/${ev.id}`}>{ev.name || ev.title || "Подія"}</Link>
                </h4>
                <div className="card__meta">
                    <span className="chip">{start || "без дати"}</span>
                    <span className="chip chip--ghost">{ev.location || "—"}</span>
                </div>
            </div>
            <div className="card__aside text-end">
                <div className="muted small mb-1">
                    Місць: {capacity} • Вільно: {free}
                </div>
                <Link to={`/events/${ev.id}`} className="btn btn-sm btn-outline-primary">
                    Деталі
                </Link>
            </div>
        </div>
    );
}

export default function DashboardPage() {
    const {user} = useAuth();
    const roles = Array.isArray(user?.roles)
        ? user.roles
        : typeof user?.roles === "string"
            ? user.roles.split(/[\s,]+/).filter(Boolean)
            : [];
    const isAdmin = roles.includes("ROLE_ADMIN") || roles.includes("ADMIN");

    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [err, setErr] = useState("");

    useEffect(() => {
        let ignore = false;
        (async () => {
            try {
                setLoading(true);
                // 1) тягнемо мої реєстрації
                const regs = await getMyRegistrations();
                if (ignore) return;

                const ids = Array.from(
                    new Set(
                        (Array.isArray(regs) ? regs : [])
                            .map((r) => r?.event?.id ?? r?.eventId)
                            .filter(Boolean)
                    )
                );

                // 2) добираємо події по кожному id (безпечніше, ніж покладатись на r.event LAZY)
                const results = [];
                for (const id of ids) {
                    try {
                        const ev = await getEventById(id);
                        results.push(ev);
                    } catch {
                        // ігноруємо падіння окремої події
                    }
                }
                setEvents(results);
            } catch (e) {
                if (!ignore) setErr(e?.message || "Не вдалося завантажити ваші івенти");
            } finally {
                if (!ignore) setLoading(false);
            }
        })();
        return () => {
            ignore = true;
        };
    }, []);

    const now = Date.now();
    const {upcoming, past} = useMemo(() => {
        const enriched = events.slice().sort(byStartAsc);
        const up = [],
            p = [];
        for (const ev of enriched) {
            const ts = new Date(ev.startAt || 0).getTime();
            (ts && ts >= now ? up : p).push(ev);
        }
        return {upcoming: up, past: p};
    }, [events, now]);

    return (
        <div className="container py-4">
            <style>{styles}</style>
            <div className="d-flex justify-content-between align-items-center mb-3">
                <h1 className="mb-0">Мій кабінет</h1>
                {isAdmin && (
                    <Link to="/events/create" className="btn btn-outline-primary">
                        Створити івент
                    </Link>
                )}
            </div>
            {err && <div className="alert alert-danger">{err}</div>}
            {loading ? (
                <div>Завантаження…</div>
            ) : (
                <div className="grid">
                    <section className="panel">
                        <h3 className="panel__title">Майбутні</h3>
                        {upcoming.length === 0 ? (
                            <div className="muted">Немає</div>
                        ) : (
                            <div className="list-group">
                                {upcoming.map((ev) => (
                                    <EventItem key={ev.id} ev={ev}/>
                                ))}
                            </div>
                        )}
                    </section>

                    <section className="panel">
                        <h3 className="panel__title">Минулі</h3>
                        {past.length === 0 ? (
                            <div className="muted">Немає</div>
                        ) : (
                            <div className="list-group">
                                {past.map((ev) => (
                                    <EventItem key={ev.id} ev={ev}/>
                                ))}
                            </div>
                        )}
                    </section>
                </div>
            )}

            <div className="mt-4 d-flex gap-2">
                <Link to="/events" className="btn btn-outline-secondary">
                    Усі івенти
                </Link>
            </div>
        </div>
    );
}

const styles = `
:root{
  --bg:#0b1020; --panel:#0f1530; --panel2:#0b1428; --ring:#4c7fff; --text:#e7eaf2; --muted:#93a0b5;
}
.container{ color:var(--text) }
.grid{ display:grid; grid-template-columns:1fr 1fr; gap:16px }
.panel{ background:var(--panel); border:1px solid #ffffff19; border-radius:16px; padding:16px }
.panel__title{ font-size:16px; color:var(--muted); margin:0 0 12px }
.list-group{ display:flex; flex-direction:column; gap:12px }
.card{ background:var(--panel2); border:1px solid #ffffff14; border-radius:14px; padding:14px }
.card--row{ display:flex; align-items:flex-start; gap:12px; justify-content:space-between }
.card__title{ margin:0; font-size:16px }
.card__meta{ display:flex; gap:8px; flex-wrap:wrap }
.chip{ display:inline-flex; align-items:center; gap:6px; font-size:12px; padding:4px 8px; border-radius:999px; background:#ffffff08; border:1px solid #ffffff12; color:var(--text) }
.chip--ghost{ color:var(--muted) }
.muted{ color:var(--muted) }
@media (max-width:900px){ .grid{ grid-template-columns:1fr } }
`;
