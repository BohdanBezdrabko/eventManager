import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function LandingPage() {
    const navigate = useNavigate();
    const { user } = useAuth();

    return (
        <>
            <style>{styles}</style>
            <div className="auth">
                <div className="panel">
                    <div className="hero">
                        <div className="logo">🏐</div>
                        <h1>Sport Admin System</h1>
                        <p className="muted">Керуйте подіями швидко і зручно</p>
                    </div>

                    <div className="actions">
                        {user ? (
                            <>
                                <button className="primary" onClick={() => navigate("/dashboard")}>
                                    Перейти в кабінет
                                </button>
                                <button className="secondary" onClick={() => navigate("/events")}>
                                    Події
                                </button>
                            </>
                        ) : (
                            <>
                                <button className="primary" onClick={() => navigate("/login")}>
                                    Увійти
                                </button>
                                <button className="secondary" onClick={() => navigate("/register")}>
                                    Зареєструватися
                                </button>
                            </>
                        )}
                    </div>
                </div>
            </div>
        </>
    );
}

const styles = `
:root{
  --bg:#0e0f13; --panel:#131722; --panel2:#1a1f2d;
  --text:#e7eaf2; --muted:#a7afc2; --ring:rgba(122,144,255,.35);
  --brand:#6aa3ff; --brand-2:#7f72ff;
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
.logo{
  width:90px; height:90px; border-radius:24px; display:grid; place-items:center;
  font-size:38px; background:linear-gradient(145deg, var(--brand), var(--brand-2));
  color:#fff; box-shadow:0 10px 24px rgba(127,114,255,.35);
}
h1{ margin:10px 0 0; font-size:24px }
.muted{ margin:4px 0 0; color:var(--muted); font-size:14px }

.actions{ display:flex; gap:12px; justify-content:center; margin-top:16px; flex-wrap:wrap }
.primary, .secondary{
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
