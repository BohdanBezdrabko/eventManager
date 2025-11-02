// src/pages/RegisterPage.jsx
import { useEffect, useState } from "react";
import { useAuth } from "@/context/AuthContext.jsx";
import { useNavigate, Link } from "react-router-dom";

export default function RegisterPage() {
    const { register, user } = useAuth();
    const [form, setForm] = useState({ username: "", password: "", role: "user" });
    const [error, setError] = useState("");
    const [showPwd, setShowPwd] = useState(false);
    const [busy, setBusy] = useState(false);
    const navigate = useNavigate();

    useEffect(() => { if (user) navigate("/dashboard", { replace: true }); }, [user, navigate]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError("");
        setBusy(true);
        try {
            await register(form.username, form.password, form.role);
            navigate("/dashboard", { replace: true });
        } catch (err) {
            setError(err?.message || "Помилка реєстрації");
        } finally {
            setBusy(false);
        }
    };

    return (
        <>
            <style>{styles}</style>
            <div className="auth">
                <div className="panel">
                    <header className="head">
                        <h1>Створення акаунта</h1>
                        <p>Зареєструйтеся та виберіть роль</p>
                    </header>

                    {error && <div className="alert">{error}</div>}

                    <form onSubmit={handleSubmit} className="form">
                        <div className="field">
                            <label>Логін</label>
                            <input
                                autoFocus
                                placeholder="username"
                                value={form.username}
                                onChange={e => setForm(f => ({ ...f, username: e.target.value }))}
                            />
                        </div>

                        <div className="field">
                            <label>Пароль</label>
                            <div className="pwd">
                                <input
                                    type={showPwd ? "text" : "password"}
                                    placeholder="••••••••"
                                    value={form.password}
                                    onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
                                />
                                <button
                                    type="button"
                                    className="ghost"
                                    onClick={() => setShowPwd(v => !v)}
                                    aria-label={showPwd ? "Сховати пароль" : "Показати пароль"}
                                >
                                    {showPwd ? "Приховати" : "Показати"}
                                </button>
                            </div>
                        </div>

                        <RolePicker
                            value={form.role}
                            onChange={(role) => setForm(f => ({ ...f, role }))}
                        />

                        <div className="actions">
                            <button className="primary" type="submit" disabled={busy || !form.username || !form.password}>
                                {busy ? "Створення…" : "Створити акаунт"}
                            </button>
                            <Link to="/login" className="link">Увійти</Link>
                        </div>
                    </form>
                </div>
            </div>
        </>
    );
}

/* ——— роль як сучасний перемикач ——— */
function RolePicker({ value, onChange }) {
    return (
        <div className="role">
            <label>Роль</label>
            <div className="role__seg">
                <button
                    type="button"
                    className={`seg ${value === "user" ? "active" : ""}`}
                    onClick={() => onChange("user")}
                >
                    <SvgUser />
                    <div>
                        <div className="seg__title">Користувач</div>
                        <div className="seg__sub">Реєстрація та участь в івентах</div>
                    </div>
                </button>

                <button
                    type="button"
                    className={`seg ${value === "admin" ? "active" : ""}`}
                    onClick={() => onChange("admin")}
                >
                    <SvgShield />
                    <div>
                        <div className="seg__title">Івент-менеджер</div>
                        <div className="seg__sub">Створення і керування подіями</div>
                    </div>
                </button>
            </div>
            <input type="hidden" name="role" value={value} readOnly />
        </div>
    );
}

/* ——— маленькі SVG ——— */
function SvgUser() {
    return (
        <svg width="22" height="22" viewBox="0 0 24 24" aria-hidden="true">
            <path fill="currentColor" d="M12 12a5 5 0 1 0-5-5a5 5 0 0 0 5 5m0 2c-5.33 0-8 2.667-8 6v1h16v-1c0-3.333-2.67-6-8-6"/>
        </svg>
    );
}
function SvgShield() {
    return (
        <svg width="22" height="22" viewBox="0 0 24 24" aria-hidden="true">
            <path fill="currentColor" d="M12 2l7 3v6c0 5-3.33 9.74-7 11c-3.67-1.26-7-6-7-11V5z"/>
        </svg>
    );
}

/* ——— стилі у файлі ——— */
const styles = `
:root{
  --bg:#0e0f13; --panel:#131722; --panel2:#1a1f2d;
  --text:#e7eaf2; --muted:#a7afc2; --ring:rgba(122,144,255,.35);
  --brand:#6aa3ff; --accent:#3ad0a1; --danger:#ff6b6b;
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
  width:100%; max-width:560px; background:linear-gradient(135deg, rgba(122,144,255,.12), rgba(58,208,161,.07)), var(--panel);
  border:1px solid #ffffff14; border-radius:16px; padding:22px 22px 20px;
  box-shadow:0 20px 40px rgba(0,0,0,.35), 0 1px 0 #ffffff12 inset;
}
.head h1{margin:0 0 6px; font-size:22px}
.head p{margin:0; color:var(--muted); font-size:14px}

.alert{
  margin-top:12px; padding:12px 14px; border-radius:12px;
  background:#3b1e1e; color:#ffd1d1; border:1px solid #ff6b6b33;
}

.form{ margin-top:16px; display:grid; gap:14px }
.field label{ display:block; margin:0 0 6px; color:var(--muted); font-size:13px }
.field input{
  width:100%; padding:12px 14px; border-radius:12px;
  background:var(--panel2); color:var(--text);
  border:1px solid #ffffff12; outline:none;
}
.field input:focus{ border-color:var(--ring); box-shadow:0 0 0 4px #7a90ff2e }

.pwd{ display:flex; gap:8px; align-items:center }
.pwd input{ flex:1 }
.ghost{
  padding:10px 12px; border-radius:10px; border:1px solid #ffffff12; background:#ffffff0a; color:var(--text);
}
.ghost:hover{ border-color:var(--ring) }

.role label{ display:block; margin:4px 0 8px; color:var(--muted); font-size:13px }
.role__seg{
  display:grid; grid-template-columns:1fr 1fr; gap:10px;
}
.seg{
  width:100%; display:flex; gap:10px; align-items:flex-start; text-align:left;
  padding:12px; border-radius:14px; background:var(--panel2); color:var(--text);
  border:1px solid #ffffff12; cursor:pointer;
  transition:transform .15s ease, border-color .15s ease, box-shadow .15s ease;
}
.seg svg{ margin-top:2px; opacity:.9 }
.seg__title{ font-weight:600; line-height:1.2 }
.seg__sub{ font-size:12px; color:var(--muted); margin-top:2px }
.seg:hover{ border-color:var(--ring); transform:translateY(-1px) }
.seg.active{
  border-color:var(--ring);
  box-shadow:0 8px 22px rgba(0,0,0,.35), 0 0 0 4px #7a90ff2a inset;
}

.actions{ display:flex; gap:10px; align-items:center; margin-top:6px }
.primary{
  padding:12px 16px; border-radius:12px; border:1px solid #7a90ff55;
  background:linear-gradient(145deg, #6aa3ff, #7f72ff); color:white; font-weight:600;
  transition:transform .1s ease, filter .1s ease;
}
.primary:disabled{ opacity:.6; cursor:not-allowed }
.primary:not(:disabled):hover{ filter:brightness(1.05); transform:translateY(-1px) }
.link{ color:var(--muted); text-decoration:none; padding:10px 8px }
.link:hover{ color:var(--text) }

@media (max-width:520px){
  .role__seg{ grid-template-columns:1fr }
  .actions{ flex-direction:column; align-items:stretch }
  .link{ text-align:center }
}
`;
