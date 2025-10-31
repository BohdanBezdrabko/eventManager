// src/pages/DashboardPage.jsx
import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { getEventsByAuthor } from "@/services/events.jsx";
import { useAuth } from "@/context/AuthContext";

function byStartAsc(a, b) {
    const ta = new Date(a.startAt || 0).getTime() || 0;
    const tb = new Date(b.startAt || 0).getTime() || 0;
    return ta - tb;
}

function EventItem({ ev }) {
    const start = ev.startAt ? new Date(ev.startAt).toLocaleString() : "";
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
                <Link to={`/events/${ev.id}`} className="btn btn-sm btn-outline-primary">Деталі</Link>
            </div>
        </div>
    );
}

export default function DashboardPage() {
    const { user, booting } = useAuth();
    const userId = user?.id ?? null;

    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [err, setErr] = useState("");

    const upcoming = useMemo(() => {
        const now = Date.now();
        return (events || [])
            .filter((e) => new Date(e.startAt || 0).getTime() >= now)
            .sort(byStartAsc);
    }, [events]);

    const past = useMemo(() => {
        const now = Date.now();
        return (events || [])
            .filter((e) => new Date(e.startAt || 0).getTime() < now)
            .sort(byStartAsc);
    }, [events]);

    useEffect(() => {
        if (booting) return;
        if (!userId) {
            setEvents([]);
            setLoading(false);
            setErr("");
            return;
        }
        let ignore = false;
        (async () => {
            try {
                setLoading(true);
                setErr("");

                // 🔹 лише свій список через ?createdBy=
                const data = await getEventsByAuthor({
                    userId,
                    page: 0,
                    size: 1000,
                    sort: "startAt,desc",
                });

                if (ignore) return;
                const items =
                    (Array.isArray(data) && data) ||
                    data?.content ||
                    data?.items ||
                    data?.results ||
                    [];
                setEvents(items);
            } catch (e) {
                if (!ignore) setErr(e?.message || "Помилка завантаження.");
            } finally {
                if (!ignore) setLoading(false);
            }
        })();
        return () => {
            ignore = true;
        };
    }, [booting, userId]);

    return (
        <div className="container py-4">
            <style>{styles}</style>
            <div className="toolbar mb-3">
                <h1 className="page-title">Мої івенти (я автор)</h1>
                <div className="toolbar__right">
                    <Link to="/events/create" className="btn btn-outline-primary">+ Створити івент</Link>
                </div>
            </div>

            {booting || loading ? (
                <div>Завантаження…</div>
            ) : err ? (
                <div className="alert alert-danger">{err}</div>
            ) : !userId ? (
                <div className="muted">Щоб побачити ваші івенти як автора — увійдіть у систему.</div>
            ) : (
                <div className="grid">
                    <section className="panel">
                        <h3 className="panel__title">Майбутні</h3>
                        {upcoming.length === 0 ? (
                            <div className="muted">Немає</div>
                        ) : (
                            <div className="list-group">
                                {upcoming.map((ev) => (
                                    <EventItem key={ev.id} ev={ev} />
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
                                    <EventItem key={ev.id} ev={ev} />
                                ))}
                            </div>
                        )}
                    </section>
                </div>
            )}
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
.chip{ display:inline-flex; align-items:center; gap:6px; font-size:12px; padding:6px 10px; border-radius:999px; background:#ffffff08; border:1px solid #ffffff12; color:var(--text) }
.chip--ghost{ color:var(--muted) }
.muted{ color:var(--muted) }
.toolbar{ display:flex; align-items:center; justify-content:space-between; gap:12px }
.page-title{ margin:0; font-size:20px }
.btn{ padding:8px 12px; border-radius:999px; border:1px solid #ffffff22; background:#ffffff0d; color:var(--text); text-decoration:none; cursor:pointer }
.btn-sm{ padding:6px 10px; font-size:12px }
.btn-outline-primary{ border-color:#4c7fff66 }
@media (max-width:900px){ .grid{ grid-template-columns:1fr } }
`;
