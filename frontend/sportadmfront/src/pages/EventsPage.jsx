import { useEffect, useMemo, useState, useCallback } from "react";
import { Link } from "react-router-dom";
import { getAllEvents } from "@/services/events.jsx";
import { registerForEvent } from "@/services/eventRegistrations.jsx";
import { useAuth } from "@/context/AuthContext";

function pick(...xs) { return xs.find(v => v !== undefined && v !== null && v !== ""); }
function parseWhen(e) { return pick(e.startAt, e.start_at, e.start, e.date, e.datetime); }
function toDateSafe(iso) { const d = new Date(iso); return Number.isNaN(d) ? null : d; }
function fmtDateTime(iso) { if (!iso) return "без дати"; const d = toDateSafe(iso); return d ? d.toLocaleString() : String(iso); }

export default function EventsPage() {
    const { user } = useAuth();
    const roles = Array.isArray(user?.roles)
        ? user.roles
        : typeof user?.roles === "string"
            ? user.roles.split(/[\s,]+/).filter(Boolean)
            : [];
    const isAdmin = roles.includes("ROLE_ADMIN") || roles.includes("ADMIN");

    const [items, setItems] = useState([]);
    const [q, setQ] = useState("");
    const [loading, setLoading] = useState(true);
    const [err, setErr] = useState("");

    useEffect(() => {
        let ignore = false;
        (async () => {
            try {
                setLoading(true);
                const data = await getAllEvents(q ? { q } : undefined);
                if (!ignore) setItems(Array.isArray(data) ? data : data?.content || []);
            } catch (e) { if (!ignore) setErr(e?.message || "Не вдалося завантажити список"); }
            finally { if (!ignore) setLoading(false); }
        })();
        return () => { ignore = true; };
    }, [q]);

    const filtered = useMemo(() => {
        if (!q) return items;
        const s = q.trim().toLowerCase();
        return items.filter(e => `${e.name||e.title||""} ${e.location||""}`.toLowerCase().includes(s));
    }, [items, q]);

    const onQuickRegister = useCallback(async (id) => {
        try {
            await registerForEvent(id);
            alert("Зареєстровано");
        } catch (e) { alert(e?.message || "Помилка реєстрації"); }
    }, []);

    return (
        <div className="container py-4">
            <style>{styles}</style>
            <div className="toolbar">
                <input className="search" placeholder="Пошук…" value={q} onChange={e=>setQ(e.target.value)} />
                {isAdmin && <Link to="/events/create" className="btn">Створити івент</Link>}
            </div>

            {err && <div className="alert alert-danger">{err}</div>}
            {loading ? (
                <div>Завантаження…</div>
            ) : (
                <div className="grid">
                    {filtered.map(ev => (
                        <div key={ev.id} className="card">
                            <h4 className="card__title"><Link to={`/events/${ev.id}`}>{ev.name || ev.title || "Подія"}</Link></h4>
                            <div className="card__meta">
                                <span className="chip">{fmtDateTime(parseWhen(ev))}</span>
                                <span className="chip chip--ghost">{ev.location || "—"}</span>
                            </div>
                            <div className="card__footer">
                                <button className="btn ghost" onClick={() => onQuickRegister(ev.id)}>Зареєструватися</button>
                                <Link to={`/events/${ev.id}`} className="btn">Детальніше</Link>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

const styles = `
:root{ --panel:#0f1530; --panel2:#0b1428; --text:#e7eaf2; --muted:#93a0b5 }
*{ box-sizing:border-box }
a{ color:inherit; text-decoration:none }

.container{ color:var(--text) }

.toolbar{ display:flex; gap:12px; margin-bottom:16px }
.search{ flex:1; min-width:280px; padding:10px 12px; border-radius:10px; border:1px solid #ffffff22; background:var(--panel2); color:var(--text) }

.grid{ display:grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap:14px }

.card{
  background:var(--panel);
  border:1px solid #ffffff19;
  border-radius:14px;
  padding:14px;
  display:flex;
  flex-direction:column;
  gap:10px;
  overflow:hidden; /* не дає елементам “вилазити” за межі */
}

.card__title{ margin:0 }
.card__meta{ display:flex; gap:8px; flex-wrap:wrap }

.chip{
  display:inline-flex; align-items:center; gap:6px;
  font-size:12px; padding:4px 8px; border-radius:999px;
  background:#ffffff08; border:1px solid #ffffff12; color:var(--text)
}
.chip--ghost{ color:var(--muted) }

.card__footer{
  margin-top:auto;
  display:flex;
  gap:8px;
  justify-content:flex-end;
  align-items:center;
  flex-wrap:wrap; /* щоб кнопки переносились, а не ламали верстку */
}

.btn{
  display:inline-flex;
  align-items:center;
  justify-content:center;
  gap:6px;
  padding:10px 14px;
  border-radius:10px;
  border:1px solid #7a90ff55;
  background:linear-gradient(145deg, #6aa3ff, #7f72ff);
  color:#fff;
  line-height:1;
  white-space:nowrap;  /* не переносити текст кнопки */
  cursor:pointer;
}

.btn.ghost{
  background:#ffffff08;
  border:1px solid #ffffff22;
}

/* мобільний UX: кнопки стають у рядок на всю ширину */
@media (max-width:560px){
  .grid{ grid-template-columns:1fr }
  .card__footer{ justify-content:stretch }
  .card__footer .btn{ flex:1 }
}
`;
