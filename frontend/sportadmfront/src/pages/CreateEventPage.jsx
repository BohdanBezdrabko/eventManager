// src/pages/CreateEventPage.jsx
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { createEvent } from "@/services/events.jsx";
import { AlertCircle } from "lucide-react";

export default function CreateEventPage() {
    const navigate = useNavigate();
    const [form, setForm] = useState({
        name: "",
        startAt: "",           // "2025-09-20T18:00"
        location: "",
        capacity: "",
        coverUrl: "",
        description: "",
    });
    const [submitting, setSubmitting] = useState(false);
    const [err, setErr] = useState(null);

    const onChange = (e) => {
        const { name, value } = e.target;
        setForm((f) => ({ ...f, [name]: value }));
    };

    const onSubmit = async (e) => {
        e.preventDefault();
        setErr(null);
        setSubmitting(true);
        try {
            const payload = {
                name: form.name.trim(),
                startAt: form.startAt ? new Date(form.startAt).toISOString().slice(0, 19) : null, // LocalDateTime
                location: form.location.trim(),
                capacity: form.capacity ? Number(form.capacity) : null,
                coverUrl: form.coverUrl?.trim() || null,
                description: form.description?.trim() || null,
            };
            const saved = await createEvent(payload);
            navigate(`/events/${saved.id}`);
        } catch (e2) {
            setErr(e2?.message || "Помилка створення");
        } finally {
            setSubmitting(false);
        }
    };

    const valid = form.name.trim() && form.startAt;

    return (
        <>
            <style>{styles}</style>
            <div className="auth">
                <div className="panel">
                    <header className="head">
                        <h1>Новий івент</h1>
                        <p>Заповніть поля та опублікуйте подію</p>
                    </header>

                    {err && (
                        <div className="alert">
                            <AlertCircle size={18} style={{ marginRight: 8, verticalAlign: "-3px" }} />
                            {err}
                        </div>
                    )}

                    <form onSubmit={onSubmit} className="form">
                        {/* верхня сітка */}
                        <div className="grid">
                            <div className="field col-2">
                                <label>Назва</label>
                                <input
                                    name="name"
                                    placeholder="Напр.: Hackathon Autumn"
                                    value={form.name}
                                    onChange={onChange}
                                    required
                                />
                            </div>

                            <div className="field">
                                <label>Дата і час</label>
                                <input
                                    name="startAt"
                                    type="datetime-local"
                                    value={form.startAt}
                                    onChange={onChange}
                                    required
                                />
                                <small className="hint">Локальний час. Збережеться у форматі LocalDateTime.</small>
                            </div>

                            <div className="field">
                                <label>Локація</label>
                                <input
                                    name="location"
                                    placeholder="Київ, просп. Перемоги 37"
                                    value={form.location}
                                    onChange={onChange}
                                    required
                                />
                            </div>

                            <div className="field">
                                <label>Місткість</label>
                                <input
                                    name="capacity"
                                    type="number"
                                    min="1"
                                    step="1"
                                    placeholder="Напр.: 100"
                                    value={form.capacity}
                                    onChange={onChange}
                                />
                            </div>

                            <div className="field col-2">
                                <label>Cover URL</label>
                                <input
                                    name="coverUrl"
                                    placeholder="https://..."
                                    value={form.coverUrl}
                                    onChange={onChange}
                                />
                                <small className="hint">Опціонально. Публічне зображення для картки події.</small>
                            </div>
                        </div>

                        <div className="field">
                            <label>Опис</label>
                            <textarea
                                name="description"
                                rows="5"
                                placeholder="Коротко про формат, спікерів, вимоги до участі…"
                                value={form.description}
                                onChange={onChange}
                            />
                        </div>

                        {/* прев’ю картки */}
                        <div className="preview">
                            <div
                                className="preview__media"
                                style={{ backgroundImage: form.coverUrl ? `url(${form.coverUrl})` : "none" }}
                            />
                            <div className="preview__body">
                                <div className="preview__title">{form.name || "Назва події"}</div>
                                <div className="preview__meta">
                                    <span className="chip">{form.startAt || "дата не вибрана"}</span>
                                    {form.location ? <span className="chip chip--ghost">{form.location}</span> : null}
                                </div>
                                {form.description ? (
                                    <div className="preview__desc">{form.description}</div>
                                ) : (
                                    <div className="preview__desc muted">Опис з’явиться тут</div>
                                )}
                            </div>
                        </div>

                        <div className="actions">
                            <button className="primary" type="submit" disabled={submitting || !valid}>
                                {submitting ? "Створення…" : "Створити"}
                            </button>
                            <button
                                type="button"
                                className="secondary"
                                onClick={() => navigate(-1)}
                                disabled={submitting}
                            >
                                Скасувати
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </>
    );
}

/* — стилі, узгоджені з Login/Register/Home — */
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
  width:100%; max-width:820px;
  background:linear-gradient(135deg, rgba(122,144,255,.12), rgba(58,208,161,.07)), var(--panel);
  border:1px solid #ffffff14; border-radius:16px; padding:24px;
  box-shadow:0 20px 40px rgba(0,0,0,.35), 0 1px 0 #ffffff12 inset;
}
.head h1{margin:0 0 6px; font-size:22px}
.head p{margin:0; color:var(--muted); font-size:14px}

.alert{
  margin:12px 0 0; padding:12px 14px; border-radius:12px;
  background:#3b1e1e; color:#ffd1d1; border:1px solid #ff6b6b33;
  display:flex; align-items:center;
}

.form{ margin-top:16px; display:grid; gap:16px }
.grid{
  display:grid; gap:12px;
  grid-template-columns: repeat(2, minmax(0,1fr));
}
.col-2{ grid-column: span 2 }
.field label{ display:block; margin:0 0 6px; color:var(--muted); font-size:13px }
.field input, .field textarea{
  width:100%; padding:12px 14px; border-radius:12px;
  background:var(--panel2); color:var(--text);
  border:1px solid #ffffff12; outline:none; resize:vertical;
}
.field input:focus, .field textarea:focus{ border-color:var(--ring); box-shadow:0 0 0 4px #7a90ff2e }
.hint{ display:block; margin-top:6px; color:var(--muted); font-size:12px }

.preview{
  display:grid; grid-template-columns: 240px 1fr; gap:12px;
  background:#0f1421; border:1px solid #ffffff12; border-radius:14px; overflow:hidden;
}
.preview__media{
  background:#10121a center/cover no-repeat; min-height:160px;
}
.preview__body{ padding:12px; display:grid; gap:8px }
.preview__title{ font-weight:600; font-size:16px }
.preview__meta{ display:flex; gap:8px; flex-wrap:wrap }
.preview__desc{ font-size:13px; line-height:1.35 }
.muted{ color:var(--muted) }
.chip{
  font-size:12px; padding:4px 8px; border-radius:999px;
  background:#ffffff08; border:1px solid #ffffff12; color:#e7eaf2;
}
.chip--ghost{ color:var(--muted) }

.actions{ display:flex; gap:10px; align-items:center; margin-top:6px; flex-wrap:wrap }
.primary, .secondary{
  padding:12px 16px; border-radius:12px; font-weight:600;
  transition:transform .1s ease, filter .1s ease, border-color .1s ease;
}
.primary{
  border:1px solid #7a90ff55;
  background:linear-gradient(145deg, #6aa3ff, #7f72ff); color:white;
}
.primary:disabled{ opacity:.6; cursor:not-allowed }
.primary:not(:disabled):hover{ filter:brightness(1.05); transform:translateY(-1px) }
.secondary{
  border:1px solid #ffffff22; background:var(--panel2); color:#e7eaf2;
}
.secondary:hover{ border-color:var(--ring); transform:translateY(-1px) }

@media (max-width:780px){
  .grid{ grid-template-columns: 1fr }
  .col-2{ grid-column: auto }
  .preview{ grid-template-columns: 1fr }
}
`;
