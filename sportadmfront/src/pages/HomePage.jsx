// src/pages/HomePage.jsx
import { useMemo } from "react";
import { useAuth } from "../context/AuthContext";
import { Link } from "react-router-dom";

/* — утиліти для відображення імені та ролі — */
function getDisplayName(user) {
    if (!user) return "";
    return user.fullName || user.name || user.username || (user.email ? user.email.split("@")[0] : "");
}
function normalizeRoles(roles) {
    if (!roles) return [];
    if (Array.isArray(roles)) return roles;
    if (typeof roles === "string") return [roles];
    if (roles && Array.isArray(roles.authorities)) return roles.authorities.map(r => r.authority ?? r);
    return [];
}
function initials(name) {
    if (!name) return "";
    const parts = name.trim().split(/\s+/).slice(0, 2);
    return parts.map(p => p[0]?.toUpperCase() || "").join("");
}

export default function HomePage() {
    const { user } = useAuth();

    const { name, roleLabel, isAdmin } = useMemo(() => {
        const n = getDisplayName(user) || "користувач";
        const roles = normalizeRoles(user?.roles).map(r => String(r).toUpperCase());
        const admin = roles.includes("ROLE_ADMIN");
        return { name: n, roleLabel: admin ? "Івент-менеджер" : "Користувач", isAdmin: admin };
    }, [user]);

    return (
        <>
            <style>{styles}</style>
            <div className="auth">
                <div className="panel">
                    <div className="hero">
                        <div className="avatar">{initials(name) || "👋"}</div>
                        <h1>Ласкаво просимо</h1>
                        <p className="muted">
                            Ви увійшли як <span className="highlight">{name}</span>
                        </p>
                        <div className={`role ${isAdmin ? "admin" : ""}`}>{roleLabel}</div>
                    </div>

                    <div className="actions">
                        <Link to="/events" className="primary">Переглянути події</Link>
                        <Link to="/dashboard" className="secondary">Кабінет користувача</Link>
                    </div>
                </div>
            </div>
        </>
    );
}

/* — стилі у файлі, спільний вигляд із Login/Register — */
const styles = `
:root{
  --bg:#0e0f13; --panel:#131722; --panel2:#1a1f2d;
  --text:#e7eaf2; --muted:#a7afc2; --ring:rgba(122,144,255,.35);
  --brand:#6aa3ff; --brand-2:#7f72ff; --accent:#3ad0a1;
}
*{box-sizing:border-box}
html,body,#root{height:100%}
.auth{
  min-height:100dvh; display:grid; place-items:center; padding:24px;
  background:
    radial-gradient(1200px 600px at 100% -10%, rgba(127,114,255,.08), transparent),
    radial-gradient(900px 500px at -10% 0%, rgba(58,208,161,.06), transparent),
    var(--bg);
  color:var(--text);
}
.panel{
  width:100%; max-width:640px;
  background:linear-gradient(135deg, rgba(122,144,255,.12), rgba(58,208,161,.07)), var(--panel);
  border:1px solid #ffffff14; border-radius:16px; padding:26px 24px 22px;
  box-shadow:0 20px 40px rgba(0,0,0,.35), 0 1px 0 #ffffff12 inset;
}

.hero{ text-align:center; display:grid; gap:10px; justify-items:center }
.avatar{
  width:86px; height:86px; border-radius:20px; display:grid; place-items:center; font-weight:800; font-size:30px;
  background:linear-gradient(145deg, var(--brand), var(--brand-2));
  color:#fff; box-shadow:0 10px 24px rgba(127,114,255,.35);
}
h1{ margin:8px 0 0; font-size:24px }
.muted{ margin:2px 0 0; color:var(--muted); font-size:14px }
.highlight{ color:#fff; font-weight:600 }
.role{
  margin-top:6px; font-size:12px; padding:4px 10px; border-radius:999px;
  border:1px solid #ffffff18; background:#ffffff0d; color:var(--muted);
}
.role.admin{ color:var(--accent); border-color:var(--ring); background:#00ff9914 }

.actions{ display:flex; gap:12px; justify-content:center; margin-top:16px; flex-wrap:wrap }
.primary, .secondary{
  display:inline-block; text-decoration:none; text-align:center;
  padding:12px 16px; border-radius:12px; min-width:200px; font-weight:600;
  transition:transform .1s ease, filter .1s ease, border-color .1s ease;
}
.primary{
  border:1px solid #7a90ff55;
  background:linear-gradient(145deg, #6aa3ff, #7f72ff); color:#fff;
}
.primary:hover{ filter:brightness(1.05); transform:translateY(-1px) }
.secondary{
  border:1px solid #ffffff22; background:var(--panel2); color:#e7eaf2;
}
.secondary:hover{ border-color:var(--ring); transform:translateY(-1px) }

@media (max-width:520px){
  .primary,.secondary{ min-width:unset; width:100% }
}
`;
