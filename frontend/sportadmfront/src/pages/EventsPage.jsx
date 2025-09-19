// src/pages/EventsPage.jsx
import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { getAllEvents } from "@/services/events";
import { Calendar, MapPin, Search, Plus } from "lucide-react";
import { useAuth } from "@/context/AuthContext.jsx";

/* — утиліти — */
function pick(...xs) { return xs.find(v => v !== undefined && v !== null && v !== ""); }
function parseWhen(e) { return pick(e.startAt, e.start_at, e.start, e.date, e.datetime); }
function toDateSafe(iso) { const d = new Date(iso); return Number.isNaN(d) ? null : d; }
function fmtDateTime(iso) {
    if (!iso) return "без дати";
    const d = toDateSafe(iso);
    return d ? d.toLocaleString() : String(iso);
}
function normalizeRoles(roles) {
    if (!roles) return [];
    if (Array.isArray(roles)) return roles;
    if (typeof roles === "string") return [roles];
    if (roles && Array.isArray(roles.authorities)) return roles.authorities.map(r => r.authority ?? r);
    return [];
}

export default function EventsPage() {
    const { user } = useAuth() || {};
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [err, setErr] = useState(null);
    const [q, setQ] = useState("");
    const [upcomingOnly, setUpcomingOnly] = useState(true);

    const isAdmin = useMemo(() => {
        const roles = normalizeRoles(user?.roles).map(r => String(r).toUpperCase());
        return roles.includes("ROLE_ADMIN");
    }, [user]);

    useEffect(() => {
        let cancelled = false;
        (async () => {
            try {
                const data = await getAllEvents();
                if (!cancelled) setEvents(Array.isArray(data) ? data : []);
            } catch (e) {
                if (!cancelled) setErr(e?.message || "Помилка");
            } finally {
                if (!cancelled) setLoading(false);
            }
        })();
        return () => { cancelled = true; };
    }, []);

    const filtered = useMemo(() => {
        const now = new Date();
        const text = q.trim().toLowerCase();
        return [...events]
            .filter(e => {
                if (!text) return true;
                const name = (pick(e.name, e.title, "") + "").toLowerCase();
                const loc = (pick(e.location, e.place, "") + "").toLowerCase();
                return name.includes(text) || loc.includes(text);
            })
            .filter(e => {
                if (!upcomingOnly) return true;
                const when = parseWhen(e);
                const d = toDateSafe(when);
                return !d || d >= now; // без дати теж показуємо
            })
            .sort((a, b) => {
                const da = toDateSafe(parseWhen(a));
                const db = toDateSafe(parseWhen(b));
                if (!da && !db) return 0;
                if (!da) return 1;
                if (!db) return -1;
                return da - db; // найближчі вгорі
            });
    }, [events, q, upcomingOnly]);

    return (
        <>
            <style>{styles}</style>
            <div className="dash">
                <header className="dash__hero">
                    <div>
                        <h1 className="dash__title">Події</h1>
                        <p className="dash__subtitle">Перегляньте список івентів та відкрийте деталі</p>
                    </div>

                    <div className="toolbar">
                        <div className="search">
                            <Search size={18} />
                            <input
                                placeholder="Пошук за назвою або локацією"
                                value={q}
                                onChange={e => setQ(e.target.value)}
                            />
                        </div>
                        <label className="toggle">
                            <input
                                type="checkbox"
                                checked={upcomingOnly}
                                onChange={e => setUpcomingOnly(e.target.checked)}
                            />
                            <span>Лише майбутні</span>
                        </label>
                        {isAdmin && (
                            <Link to="/events/new" className="btn primary">
                                <Plus size={18} /> Створити івент
                            </Link>
                        )}
                    </div>
                </header>

                <section className="dash__section">
                    {loading && <div className="state">Завантаження…</div>}
                    {err && <div className="state state--error">Помилка: {String(err)}</div>}

                    {!loading && !err && filtered.length === 0 && (
                        <div className="state">Івентів не знайдено</div>
                    )}

                    <div className="grid">
                        {filtered.map(e => {
                            const cover = pick(e.coverUrl, e.cover_url, e.cover, e.imageUrl, e.image, "");
                            const when = parseWhen(e);
                            const location = pick(e.location, e.place, "—");
                            const id = e.id ?? e.uuid ?? e._id;
                            const title = pick(e.name, e.title, "Подія");

                            return (
                                <article className="card" key={String(id) + String(when) + title}>
                                    <div
                                        className="card__media"
                                        style={{ backgroundImage: cover ? `url(${cover})` : "none" }}
                                    />
                                    <div className="card__body">
                                        <h3 className="card__title">{title}</h3>
                                        <div className="card__meta">
                      <span className="chip">
                        <Calendar size={14} /> {fmtDateTime(when)}
                      </span>
                                            <span className="chip chip--ghost">
                        <MapPin size={14} /> {location}
                      </span>
                                        </div>
                                        <div className="card__footer">
                                            <Link to={`/events/${id}`} className="btn ghost">Деталі</Link>
                                        </div>
                                    </div>
                                </article>
                            );
                        })}
                    </div>
                </section>
            </div>
        </>
    );
}

/* — стилі — */
const styles = `
:root{
  --bg:#0e0f13; --panel:#151821; --panel2:#1b2030;
  --text:#e8eaf0; --muted:#a7afc2;
  --brand:#6aa3ff; --brand-2:#7f72ff; --accent:#3ad0a1;
  --ring:rgba(122,144,255,0.35); --danger:#ff6b6b;
}
*{box-sizing:border-box}
.dash{
  color:var(--text); padding:24px; min-height:100dvh;
  background:
    radial-gradient(1200px 600px at 100% -10%, rgba(127,114,255,0.08), transparent),
    radial-gradient(900px 500px at -10% 0%, rgba(58,208,161,0.06), transparent),
    var(--bg);
}
.dash__hero{
  display:flex; gap:16px; align-items:center; justify-content:space-between;
  padding:18px 20px; border-radius:16px;
  background:linear-gradient(135deg, rgba(122,144,255,0.12), rgba(58,208,161,0.08)), var(--panel);
  box-shadow:0 1px 0 #ffffff12 inset, 0 10px 30px rgba(0,0,0,0.25);
}
.dash__title{ font-size:22px; margin:0 0 4px }
.dash__subtitle{ margin:0; color:var(--muted); font-size:14px }

.toolbar{ display:flex; gap:10px; align-items:center; flex-wrap:wrap }
.search{
  display:flex; align-items:center; gap:8px; padding:8px 10px; min-width:260px;
  background:var(--panel2); border:1px solid #ffffff12; border-radius:12px;
}
.search input{
  background:transparent; border:none; outline:none; color:var(--text); width:220px;
}
.toggle{ display:flex; align-items:center; gap:8px; color:var(--muted); font-size:13px }
.toggle input{ accent-color:#7a90ff }

.btn{
  display:inline-flex; align-items:center; gap:8px;
  padding:10px 12px; border-radius:12px; font-weight:600; text-decoration:none;
  border:1px solid #ffffff22; background:var(--panel2); color:var(--text);
  transition:transform .1s ease, border-color .1s ease, filter .1s ease;
}
.btn:hover{ border-color:var(--ring); transform:translateY(-1px) }
.btn.primary{
  border:1px solid #7a90ff55;
  background:linear-gradient(145deg, #6aa3ff, #7f72ff); color:#fff;
}

.dash__section{ margin-top:18px }
.state{
  padding:14px 16px; border-radius:12px; background:var(--panel);
  border:1px solid #ffffff10; color:var(--muted);
}
.state--error{ color:#ffd1d1; background:#3b1e1e; border-color:#ff6b6b33 }

.grid{
  margin-top:14px;
  display:grid; gap:14px;
  grid-template-columns:repeat(auto-fill, minmax(260px, 1fr));
}

.card{
  background:var(--panel);
  border:1px solid #ffffff10;
  border-radius:16px; overflow:hidden;
  display:grid; grid-template-rows:140px 1fr;
  transition:transform .2s ease, box-shadow .2s ease, border-color .2s ease;
}
.card:hover{ transform:translateY(-2px); border-color:var(--ring); box-shadow:0 10px 28px rgba(0,0,0,0.35) }
.card__media{ background:#10121a center/cover no-repeat }
.card__body{ padding:12px 12px 14px; display:grid; gap:10px }
.card__title{ margin:0; font-size:16px }
.card__meta{ display:flex; gap:8px; flex-wrap:wrap }
.chip{
  display:inline-flex; align-items:center; gap:6px;
  font-size:12px; padding:4px 8px; border-radius:999px;
  background:#ffffff08; border:1px solid #ffffff12; color:var(--text);
}
.chip--ghost{ color:var(--muted) }
.card__footer{ margin-top:auto; display:flex; justify-content:flex-end }
.btn.ghost{
  border:1px solid #ffffff22; background:#ffffff08;
}

@media (max-width:560px){
  .search{ min-width:unset; width:100% }
}
`;
