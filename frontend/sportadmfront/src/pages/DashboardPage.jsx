// src/pages/DashboardPage.jsx
import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext.jsx";
import { getUserEvents } from "../services/eventRegistrations.js";

/* ======== локальні утиліти ======== */
function normalizeRoles(roles) {
    if (!roles) return [];
    if (Array.isArray(roles)) return roles;
    if (typeof roles === "string") return [roles];
    if (roles && Array.isArray(roles.authorities))
        return roles.authorities.map(r => r.authority ?? r);
    return [];
}
function getDisplayName(user) {
    if (!user) return "";
    return (
        user.fullName ||
        user.name ||
        user.username ||
        (user.email ? user.email.split("@")[0] : "")
    );
}
function initials(name) {
    if (!name) return "";
    const parts = name.trim().split(/\s+/).slice(0, 2);
    return parts.map(p => p[0]?.toUpperCase() || "").join("");
}

/* ======== підкомпоненти в одному файлі ======== */
function UserBadge({ user }) {
    const name = getDisplayName(user);
    const roles = normalizeRoles(user?.roles).map(r => String(r).toUpperCase());
    const isAdmin = roles.includes("ROLE_ADMIN");
    const roleLabel = isAdmin ? "Івент-менеджер" : "Користувач";

    return (
        <div className="userbadge">
            <div className="userbadge__avatar" aria-hidden="true">
                {initials(name)}
            </div>
            <div className="userbadge__meta">
                <div className="userbadge__name">{name || "Без імені"}</div>
                <div className={`userbadge__role ${isAdmin ? "admin" : "user"}`}>
                    {roleLabel}
                </div>
            </div>
        </div>
    );
}

function EventCard({ ev }) {
    const start = ev.startAt || ev.start_at || ev.start || ev.date;
    const when = start ? new Date(start).toLocaleString() : "без дати";
    const cover = ev.coverUrl || ev.cover_url || "";
    return (
        <article className="card">
            <div
                className="card__media"
                style={{ backgroundImage: cover ? `url(${cover})` : "none" }}
            />
            <div className="card__body">
                <h3 className="card__title">{ev.name || "Подія"}</h3>
                <p className="card__desc">{ev.description || "Опис відсутній"}</p>
                <div className="card__meta">
                    <span className="chip">{when}</span>
                    {ev.location ? <span className="chip chip--ghost">{ev.location}</span> : null}
                </div>
            </div>
        </article>
    );
}

/* ======== сторінка ======== */
export default function DashboardPage() {
    const { user, booting } = useAuth();
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        if (booting) return;
        if (!user) { setLoading(false); return; }

        (async () => {
            try {
                const data = await getUserEvents(user);
                setEvents(Array.isArray(data) ? data : []);
            } catch (e) {
                console.error("Помилка завантаження івентів:", e);
                setError(e?.message || "Помилка");
            } finally {
                setLoading(false);
            }
        })();
    }, [booting, user]);

    return (
        <>
            {/* вбудовані стилі у одному файлі */}
            <style>{styles}</style>

            <div className="dash">
                <header className="dash__hero">
                    <div className="dash__hero__content">
                        <h1 className="dash__title">Мій кабінет</h1>
                        <p className="dash__subtitle">Персональна інформація та події</p>
                    </div>
                    <UserBadge user={user} />
                </header>

                <section className="dash__section">
                    <h2 className="dash__section__title">Мої події</h2>

                    {booting && <div className="state">Ініціалізація…</div>}
                    {loading && !booting && <div className="state">Завантаження…</div>}
                    {error && <div className="state state--error">Помилка: {error}</div>}
                    {!loading && !error && events.length === 0 && (
                        <div className="state">Подій немає</div>
                    )}

                    <div className="grid">
                        {events.map(ev => (
                            <EventCard key={ev.id || ev.name || Math.random()} ev={ev} />
                        ))}
                    </div>
                </section>
            </div>
        </>
    );
}

/* ======== CSS як рядок ======== */
const styles = `
:root{
  --bg:#0e0f13; --panel:#151821; --panel-2:#1b2030;
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
  display:flex; justify-content:space-between; align-items:center; gap:16px;
  padding:18px 20px; border-radius:16px;
  background:linear-gradient(135deg, rgba(122,144,255,0.12), rgba(58,208,161,0.08)), var(--panel);
  box-shadow:0 1px 0 #ffffff12 inset, 0 10px 30px rgba(0,0,0,0.25);
}
.dash__title{font-size:22px; margin:0 0 4px}
.dash__subtitle{margin:0; color:var(--muted); font-size:14px}
.dash__section{margin-top:20px}
.dash__section__title{font-size:16px; margin:0 0 12px; color:var(--muted)}
.state{
  padding:14px 16px; border-radius:12px; background:var(--panel);
  border:1px solid #ffffff10; color:var(--muted);
}
.state--error{ color:#ffd1d1; background:#3b1e1e; border-color:#ff6b6b33 }

.userbadge{
  display:flex; align-items:center; gap:12px;
  padding:8px 10px; border-radius:14px; background:var(--panel-2);
  border:1px solid #ffffff10;
}
.userbadge__avatar{
  width:40px; height:40px; border-radius:12px; display:grid; place-items:center;
  background:linear-gradient(145deg, var(--brand), var(--brand-2));
  color:white; font-weight:700; box-shadow:0 6px 14px rgba(127,114,255,0.35);
}
.userbadge__meta{ display:grid; gap:2px }
.userbadge__name{ font-weight:600; line-height:1.2 }
.userbadge__role{
  font-size:12px; width:fit-content; padding:3px 8px; border-radius:999px;
  border:1px solid #ffffff12; background:#ffffff06; color:var(--muted);
}
.userbadge__role.admin{ color:var(--accent); border-color:var(--ring); background:#00ff990d }

.grid{
  margin-top:14px;
  display:grid;
  grid-template-columns:repeat(auto-fill, minmax(260px, 1fr));
  gap:14px;
}
.card{
  background:var(--panel); border:1px solid #ffffff10; border-radius:16px;
  overflow:hidden; display:grid; grid-template-rows:140px 1fr;
  transition:transform .2s ease, box-shadow .2s ease, border-color .2s ease;
}
.card:hover{ transform:translateY(-2px); border-color:var(--ring); box-shadow:0 10px 28px rgba(0,0,0,0.35) }
.card__media{ background:#10121a center/cover no-repeat }
.card__body{ padding:12px 12px 14px; display:grid; gap:8px }
.card__title{ margin:0; font-size:16px }
.card__desc{
  margin:0; color:var(--muted); font-size:13px; line-height:1.35;
  display:-webkit-box; -webkit-line-clamp:3; -webkit-box-orient:vertical; overflow:hidden;
}
.card__meta{ display:flex; gap:8px; flex-wrap:wrap }
.chip{
  font-size:12px; padding:4px 8px; border-radius:999px;
  background:#ffffff08; border:1px solid #ffffff12; color:var(--text);
}
.chip--ghost{ color:var(--muted) }
`;
