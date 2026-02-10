// ===============================
// File: src/pages/DashboardPage.jsx
// ===============================
import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { getEventsByAuthor } from "@/services/events.jsx";
import { useAuth } from "@/context/AuthContext";

function normalizePage(resp) {
    if (!resp || typeof resp !== "object") return { content: [], totalElements: 0 };
    return {
        content: resp.content ?? resp.items ?? resp.results ?? resp.data ?? resp.list ?? [],
        totalElements: resp.totalElements ?? resp.total ?? (resp.content ? resp.content.length : 0),
    };
}

function byStartAsc(a, b) {
    const ta = new Date(a.startAt || 0).getTime() || 0;
    const tb = new Date(b.startAt || 0).getTime() || 0;
    return ta - tb;
}

export default function DashboardPage() {
    const { user, booting } = useAuth();
    const userId = user?.id ?? null;

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [page, setPage] = useState(0);
    const [size] = useState(20);
    const [events, setEvents] = useState([]);

    const refresh = async (p = page) => {
        if (!userId) return;
        try {
            setLoading(true);
            setError("");
            const res = await getEventsByAuthor({ userId, page: p, size, sort: "startAt,desc" });
            const { content } = normalizePage(res);
            setEvents(content);
        } catch (e) {
            setError(e?.message || "Не вдалося завантажити івенти");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (userId) refresh(0);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [userId]);

    const now = useMemo(() => Date.now(), []);

    const upcoming = useMemo(
        () => events
            .filter((e) => new Date(e.startAt || 0).getTime() >= now)
            .sort(byStartAsc),
        [events, now]
    );

    const past = useMemo(
        () => events
            .filter((e) => new Date(e.startAt || 0).getTime() < now)
            .sort(byStartAsc),
        [events, now]
    );

    return (
        <div className="container py-4">
            <style>{styles}</style>

            <div className="toolbar">
                <h1 className="page-title">Мій дашборд</h1>
                <div className="toolbar__right">
                    {userId && <Link className="btn btn-outline-primary" to="/events/create">+ Створити івент</Link>}
                </div>
            </div>

            {booting || loading ? (
                <div className="muted">Завантаження…</div>
            ) : error ? (
                <div className="alert alert-danger">{error}</div>
            ) : !userId ? (
                <div className="muted">Щоб побачити ваші івенти як автора — увійдіть у систему.</div>
            ) : (
                <div className="grid">
                    <section className="panel">
                        <h3 className="panel__title">Майбутні</h3>
                        {upcoming.length === 0 ? (
                            <div className="muted">Немає запланованих івентів.</div>
                        ) : (
                            <ul className="list">
                                {upcoming.map((ev) => (
                                    <li key={ev.id} className="card card--row">
                                        <div>
                                            <h4 className="card__title">
                                                <Link to={`/events/${ev.id}`}>{ev.name}</Link>
                                            </h4>
                                            <div className="card__meta">
                                                <span className="chip">{new Date(ev.startAt).toLocaleString()}</span>
                                                {ev.location && <span className="chip chip--ghost">{ev.location}</span>}
                                                {ev.capacity ? <span className="chip chip--ghost">{ev.capacity} місць</span> : null}
                                            </div>
                                        </div>
                                        <div className="card__actions">
                                            <Link className="btn btn-sm" to={`/events/${ev.id}`}>Відкрити</Link>
                                            <Link className="btn btn-sm" to={`/events/${ev.id}/posts/create`}>+ Пост</Link>
                                        </div>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </section>

                    <section className="panel">
                        <h3 className="panel__title">Минулі</h3>
                        {past.length === 0 ? (
                            <div className="muted">Ще немає минулих івентів.</div>
                        ) : (
                            <ul className="list">
                                {past.map((ev) => (
                                    <li key={ev.id} className="card card--row">
                                        <div>
                                            <h4 className="card__title">
                                                <Link to={`/events/${ev.id}`}>{ev.name}</Link>
                                            </h4>
                                            <div className="card__meta">
                                                <span className="chip">{ev.startAt ? new Date(ev.startAt).toLocaleString() : "—"}</span>
                                                {ev.location && <span className="chip chip--ghost">{ev.location}</span>}
                                            </div>
                                        </div>
                                        <div className="card__actions">
                                            <Link className="btn btn-sm" to={`/events/${ev.id}`}>Деталі</Link>
                                        </div>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </section>
                </div>
            )}
        </div>
    );
}

const styles = `
:root{
  --bg:#0b1118; --panel:#0e1622; --panel2:#111b29; --text:#e8eef6; --muted:#9fb2c7; --accent:#5b8cff; --accent-2:#4c7fff66;
}
.container{ max-width:1100px; margin:0 auto; padding:24px }
.toolbar{ display:flex; align-items:center; justify-content:space-between; gap:12px; margin-bottom:16px }
.page-title{ margin:0; font-size:22px }
.grid{ display:grid; grid-template-columns:1fr 1fr; gap:16px }
.panel{ background:var(--panel); border:1px solid #ffffff14; border-radius:14px; padding:14px }
.panel__title{ margin:0 0 8px 0; font-size:16px; color:var(--text) }
.list{ list-style:none; padding:0; margin:0; display:flex; flex-direction:column; gap:10px }
.card{ background:var(--panel2); border:1px solid #ffffff14; border-radius:14px; padding:14px }
.card--row{ display:flex; align-items:flex-start; gap:12px; justify-content:space-between }
.card__title{ margin:0; font-size:16px }
.card__meta{ display:flex; gap:8px; flex-wrap:wrap }
.card__actions{ display:flex; gap:8px }
.chip{ display:inline-flex; align-items:center; gap:6px; font-size:12px; padding:4px 8px; border-radius:999px; background:#ffffff10; border:1px solid #ffffff12; color:var(--text) }
.chip--ghost{ color:var(--muted) }
.muted{ color:var(--muted) }
.btn{ padding:8px 12px; border-radius:999px; border:1px solid #ffffff2a; background:#121c2b; text-decoration:none; cursor:pointer; color:var(--text) }
.btn-sm{ padding:6px 10px; font-size:12px }
.btn-outline-primary{ border-color:#4c7fff66 }
.alert{ padding:12px; border-radius:10px }
.alert-danger{ background:#3b0f14; border:1px solid #a83a46; color:#ffd5d8 }
@media (max-width:900px){ .grid{ grid-template-columns:1fr } }
`;
