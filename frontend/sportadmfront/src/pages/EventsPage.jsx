import { useEffect, useState, useMemo, useRef } from "react";
import { Link } from "react-router-dom";
import { getAllEvents } from "@/services/events.jsx";
import { useAuth } from "@/context/AuthContext";

function fmt(dt) { return dt ? new Date(dt).toLocaleString() : "—"; }
const DEBOUNCE_MS = 350;

// 🔧 Нормалізація відповіді бекенда (Page/масив/різні ключі)
function normalizeList(d) {
    if (Array.isArray(d)) return d;
    if (!d || typeof d !== "object") return [];
    return d.content ?? d.items ?? d.results ?? d.data ?? d.list ?? [];
}

export default function EventsPage() {
    const { user } = useAuth();
    const roles = useMemo(() => {
        const r = Array.isArray(user?.roles)
            ? user.roles
            : typeof user?.roles === "string"
                ? user.roles.split(/[\s,]+/).filter(Boolean)
                : [];
        return r;
    }, [user]);

    const isAdmin = roles.includes("ADMIN") || roles.includes("SUPER_ADMIN");

    const [items, setItems] = useState([]);
    const [q, setQ] = useState("");
    const [loading, setLoading] = useState(true);
    const [err, setErr] = useState("");
    const timerRef = useRef(null);

    useEffect(() => {
        let alive = true;

        async function fetchAll() {
            const data = await getAllEvents();
            return normalizeList(data); // ⬅️ було data.items || []
        }

        async function fetchByName(name) {
            // 1) Пробуємо бекендовий пошук /by-name/{name}
            try {
                const resp = await fetch(`/api/v1/events/by-name/${encodeURIComponent(name)}`, {
                    headers: { "Accept": "application/json" },
                });
                if (resp.ok) {
                    const json = await resp.json();
                    return normalizeList(json); // ⬅️ було json.items || []
                }
            } catch {
                // ігноруємо — перейдемо на фолбек
            }

            // 2) Фолбек: тягнемо всі й фільтруємо локально
            const all = await fetchAll();
            const qLower = name.toLowerCase();
            return all.filter(e => String(e?.name || "").toLowerCase().includes(qLower));
        }

        function run() {
            (async () => {
                try {
                    setErr("");
                    setLoading(true);

                    const query = q.trim();
                    let list;
                    if (query.length === 0) {
                        list = await fetchAll();
                    } else if (query.length >= 2) {
                        list = await fetchByName(query);
                    } else {
                        // надто короткий запит — не мучимо бекенд, просто нічого не шукаємо
                        list = await fetchAll();
                    }

                    if (!alive) return;
                    setItems(list);
                } catch (e) {
                    if (!alive) return;
                    setErr(e?.message || "Не вдалося отримати список івентів.");
                } finally {
                    if (alive) setLoading(false);
                }
            })();
        }

        // debounce
        clearTimeout(timerRef.current);
        timerRef.current = setTimeout(run, DEBOUNCE_MS);

        return () => {
            alive = false;
            clearTimeout(timerRef.current);
        };
    }, [q]);

    return (
        <div className="container py-4">
            <style>{styles}</style>

            <div className="toolbar">
                <h1 className="page-title">Івенти</h1>
                <div className="toolbar__right">
                    <input
                        className="input"
                        placeholder="Пошук…"
                        value={q}
                        onChange={(e) => setQ(e.target.value)}
                    />
                    {isAdmin && <Link className="btn btn-outline-primary" to="/events/create">+ Івент</Link>}
                </div>
            </div>

            <div className="panel">
                {loading ? (
                    <div className="muted py-2">Завантаження…</div>
                ) : err ? (
                    <div className="alert alert-danger">{err}</div>
                ) : items.length === 0 ? (
                    <div className="muted py-2">Нічого не знайдено.</div>
                ) : (
                    <div className="list">
                        {items.map((e) => (
                            <div className="card card--row" key={e.id}>
                                <div className="card__main">
                                    <h4 className="card__title">
                                        <Link to={`/events/${e.id}`}>{e.name || `Івент #${e.id}`}</Link>
                                    </h4>
                                    <div className="card__meta">
                                        <span className="chip">{fmt(e.startAt)}</span>
                                        <span className="chip chip--ghost">{e.location || "—"}</span>
                                    </div>
                                </div>
                                <div className="card__aside">
                                    <div className="buttons">
                                        <Link className="btn btn-outline-primary btn-sm" to={`/events/${e.id}`}>Деталі</Link>
                                        {isAdmin && (
                                            <Link className="btn btn-ghost btn-sm" to={`/events/${e.id}/posts/create`}>+ Пост</Link>
                                        )}
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}

const styles = `
:root{
  --bg:#0b121a; --panel:#0f1318; --panel-2:#121922; --line:#1f2a37;
  --text:#e9f0f6; --muted:#9fb3c8; --accent:#2f88ff; --accent-2:#2473da;
  --radius:14px;
}
.container{ color:var(--text) }
.page-title{ margin:0 }
.toolbar{ display:flex; align-items:center; justify-content:space-between; gap:12px; margin-bottom:16px }
.toolbar__right{ display:flex; align-items:center; gap:10px }
.input{
  background:var(--panel-2); border:1px solid #ffffff1a; color:var(--text);
  padding:10px 12px; border-radius:10px; outline:none; min-width:220px
}
.input::placeholder{ color:var(--muted) }
.panel{ background:var(--panel); border:1px solid #ffffff19; border-radius:16px; padding:16px }
.list{ display:flex; flex-direction:column; gap:12px }
.card{ background:var(--panel-2); border:1px solid #ffffff14; border-radius:var(--radius); padding:14px }
.card--row{ display:flex; align-items:flex-start; gap:12px; justify-content:space-between }
.card__title{ margin:0 0 4px 0; font-size:16px }
.card__meta{ display:flex; gap:8px; flex-wrap:wrap }
.chip{ display:inline-flex; align-items:center; gap:6px; font-size:12px; padding:6px 8px; border-radius:999px;
  background:#ffffff08; border:1px solid #ffffff12; color:var(--text) }
.chip--ghost{ color:var(--muted) }
.buttons{ display:flex; gap:8px; justify-content:flex-end }
.btn{
  display:inline-flex; align-items:center; justify-content:center; gap:8px; padding:8px 12px;
  border-radius:10px; border:1px solid #ffffff22; cursor:pointer; text-decoration:none;
  font-weight:600; font-size:14px; transition:transform .06s ease, background .15s ease, border-color .15s ease; user-select:none; color:var(--text)
}
.btn-sm{ padding:6px 10px; font-size:13px }
.btn:active{ transform:translateY(1px) }
.btn-outline-primary{ background:transparent; border-color:var(--accent-2) }
.btn-outline-primary:hover{ background:#0f1a2a }
.btn-ghost{ background:#ffffff10; border-color:#ffffff20 }
.btn-ghost:hover{ background:#ffffff18 }
.muted{ color:var(--muted) }
.alert{ padding:12px; border-radius:10px }
.alert-danger{ background:#3b0f14; border:1px solid #a83a46; color:#ffd5d8 }
@media (max-width:720px){ .toolbar{ flex-direction:column; align-items:stretch } .toolbar__right{ justify-content:space`;
