// ===============================
// File: src/pages/CreateEventPage.jsx
// ===============================
import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { createEvent } from "@/services/events.jsx";

function toIsoFromLocal(local) {
    if (!local) return null;
    const d = new Date(local);
    return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString();
}

export default function CreateEventPage() {
    const navigate = useNavigate();
    const [form, setForm] = useState({
        name: "",
        startAt: "",
        location: "",
        capacity: "",
        coverUrl: "",
        category: "",
        tags: "",
        description: "",
    });
    const [submitting, setSubmitting] = useState(false);
    const [err, setErr] = useState("");

    const onChange = (e) => {
        const { name, value } = e.target;
        setForm((s) => ({ ...s, [name]: value }));
    };

    const onSubmit = async (e) => {
        e.preventDefault();
        try {
            setSubmitting(true);
            setErr("");
            const payload = {
                name: form.name.trim(),
                startAt: form.startAt ? toIsoFromLocal(form.startAt) : null,
                location: form.location.trim(),
                capacity: form.capacity ? Number(form.capacity) : null,
                coverUrl: form.coverUrl?.trim() || null,
                category: form.category?.trim() || null,
                tags: form.tags ? form.tags.split(",").map((t) => t.trim()).filter(Boolean) : null,
                description: form.description?.trim() || null,
            };
            const created = await createEvent(payload);
            navigate(`/events/${created.id}`);
        } catch (e) {
            setErr(e?.message || "Не вдалося створити івент");
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="container py-4">
            <style>{ceStyles}</style>

            <div className="toolbar">
                <h1 className="page-title">Створення івенту</h1>
                <div className="toolbar__right">
                    <Link to="/events" className="btn btn-ghost">← До списку</Link>
                </div>
            </div>

            <div className="cep-card">
                {err && <div className="alert alert-danger">{err}</div>}

                <form onSubmit={onSubmit} className="cep-form">
                    <div className="cep-row">
                        <label htmlFor="name" className="cep-label">Назва</label>
                        <input id="name" name="name" required value={form.name} onChange={onChange} className="cep-input" placeholder="Назва події" />
                    </div>

                    <div className="cep-row">
                        <label htmlFor="startAt" className="cep-label">Початок</label>
                        <input id="startAt" name="startAt" type="datetime-local" value={form.startAt} onChange={onChange} className="cep-input" />
                    </div>

                    <div className="cep-grid">
                        <div className="cep-row">
                            <label htmlFor="location" className="cep-label">Локація</label>
                            <input id="location" name="location" value={form.location} onChange={onChange} className="cep-input" placeholder="Місто, адреса або назва залу" />
                        </div>
                        <div className="cep-row">
                            <label htmlFor="capacity" className="cep-label">Вмістимість</label>
                            <input id="capacity" name="capacity" type="number" min="0" value={form.capacity} onChange={onChange} className="cep-input" placeholder="Напр. 150" />
                        </div>
                    </div>

                    <div className="cep-grid">
                        <div className="cep-row">
                            <label htmlFor="coverUrl" className="cep-label">Обкладинка (URL)</label>
                            <input id="coverUrl" name="coverUrl" value={form.coverUrl} onChange={onChange} className="cep-input" placeholder="https://…" />
                        </div>
                        <div className="cep-row">
                            <label htmlFor="category" className="cep-label">Категорія</label>
                            <input id="category" name="category" value={form.category} onChange={onChange} className="cep-input" placeholder="Напр. meetup" />
                        </div>
                    </div>

                    <div className="cep-row">
                        <label htmlFor="tags" className="cep-label">Теги</label>
                        <input id="tags" name="tags" value={form.tags} onChange={onChange} className="cep-input" placeholder="через кому: tech, java, spring" />
                    </div>

                    <div className="cep-row">
                        <label htmlFor="description" className="cep-label">Опис</label>
                        <textarea id="description" name="description" rows={5} value={form.description} onChange={onChange} className="cep-input" placeholder="Короткий опис заходу" />
                    </div>

                    <div className="cep-actions">
                        <Link to="/events" className="btn btn-ghost">Скасувати</Link>
                        <button className="btn btn-primary" disabled={submitting}>
                            {submitting ? "Створення…" : "Створити івент"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

const ceStyles = `
.container{ max-width:900px; margin:0 auto; padding:24px }
.toolbar{ display:flex; align-items:center; justify-content:space-between; gap:12px; margin-bottom:16px }
.page-title{ margin:0; font-size:22px }
.cep-card{ background:#0e1622; border:1px solid #ffffff14; border-radius:14px; padding:20px }
.cep-form{ display:flex; flex-direction:column; gap:14px }
.cep-grid{ display:grid; grid-template-columns:1fr 1fr; gap:14px }
.cep-row{ display:flex; flex-direction:column; gap:6px }
.cep-label{ font-weight:600; font-size:13px; color:#9fb2c7 }
.cep-input{ height:40px; padding:8px 12px; border-radius:10px; border:1px solid #ffffff22; background:#0f1826; color:#e8eef6 }
textarea.cep-input{ height:auto; resize:vertical; padding-top:10px }
.cep-actions{ display:flex; justify-content:flex-end; gap:10px; margin-top:6px }
.btn{ display:inline-flex; align-items:center; justify-content:center; gap:8px; padding:8px 12px; border-radius:10px; border:1px solid #ffffff22; cursor:pointer; text-decoration:none; font-weight:600; font-size:14px; color:#e8eef6 }
.btn-primary{ background:#1a2740; border-color:#4c7fff66 }
.btn-ghost{ background:#0f1318; border-color:#ffffff1a }
.alert{ padding:12px; border-radius:10px }
.alert-danger{ background:#3b0f14; border:1px solid #a83a46; color:#ffd5d8 }
@media (max-width:760px){ .cep-grid{ grid-template-columns:1fr } }
`;
