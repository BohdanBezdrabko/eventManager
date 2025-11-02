// ===============================
// File: src/pages/EditEventPage.jsx
// ===============================
import { useEffect, useState, useCallback } from "react";
import { useNavigate, useParams, Link } from "react-router-dom";
import { getEventById, updateEvent, deleteEvent } from "@/services/events.jsx";

function toIsoFromLocal(local) {
    if (!local) return null;
    const d = new Date(local);
    return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString();
}
function toLocalInputValue(iso) {
    if (!iso) return "";
    const d = new Date(iso);
    const pad = (n) => String(n).padStart(2, "0");
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export default function EditEventPage() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [loading, setLoading] = useState(true);
    const [err, setErr] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [deleting, setDeleting] = useState(false);

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

    const fetchEvent = useCallback(async () => {
        try {
            setErr("");
            setLoading(true);
            const ev = await getEventById(id);
            setForm({
                name: ev?.name ?? "",
                startAt: toLocalInputValue(ev?.startAt),
                location: ev?.location ?? "",
                capacity: ev?.capacity ?? "",
                coverUrl: ev?.coverUrl ?? "",
                category: ev?.category ?? "",
                tags: Array.isArray(ev?.tags) ? ev.tags.join(", ") : "",
                description: ev?.description ?? "",
            });
        } catch (e) {
            setErr(e?.message || "Не вдалося завантажити подію");
        } finally {
            setLoading(false);
        }
    }, [id]);

    useEffect(() => {
        fetchEvent();
    }, [fetchEvent]);

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
                // обовʼязкові
                name: form.name.trim(),
                startAt: form.startAt ? toIsoFromLocal(form.startAt) : null,
                // не обовʼязкові
                location: form.location?.trim() || null,
                capacity: form.capacity !== "" && form.capacity != null ? Number(form.capacity) : null,
                description: form.description?.trim() || null,
                coverUrl: form.coverUrl?.trim() || null,
                category: form.category?.trim() || null,
                tags: form.tags ? form.tags.split(",").map((t) => t.trim()).filter(Boolean) : null,
                // createdBy / createdByUsername не відправляємо
            };

            const updated = await updateEvent(id, payload);
            navigate(`/events/${updated?.id ?? id}`);
        } catch (e) {
            setErr(e?.message || "Не вдалося зберегти зміни");
        } finally {
            setSubmitting(false);
        }
    };

    const onDelete = async () => {
        if (deleting || submitting) return;
        const ok = window.confirm("Точно видалити цю подію? Дію не можна скасувати.");
        if (!ok) return;
        try {
            setDeleting(true);
            setErr("");
            await deleteEvent(id);
            navigate("/events");
        } catch (e) {
            setErr(e?.message || "Не вдалося видалити подію");
        } finally {
            setDeleting(false);
        }
    };

    if (loading) {
        return (
            <div className="container">
                <div className="toolbar">
                    <h1 className="page-title">Зміна події</h1>
                    <div className="toolbar__right">
                        <button className="btn btn-danger" disabled>
                            Видалити
                        </button>
                        <Link to={`/events/${id}`} className="btn btn-ghost">← До події</Link>
                    </div>
                </div>
                <div className="muted">Завантаження…</div>
                <style>{styles}</style>
            </div>
        );
    }

    return (
        <div className="container">
            <div className="toolbar">
                <h1 className="page-title">Зміна події</h1>
                <div className="toolbar__right">
                    {/* Кнопка видалення у тулбарі праворуч від заголовка */}
                    <button
                        type="button"
                        className="btn btn-danger"
                        onClick={onDelete}
                        disabled={deleting || submitting}
                        title="Видалити подію"
                    >
                        {deleting ? "Видалення…" : "Видалити"}
                    </button>
                    <Link to={`/events/${id}`} className="btn btn-ghost">← До події</Link>
                </div>
            </div>

            <div className="cep-card">
                {err && <div className="alert alert-danger">{err}</div>}

                <form onSubmit={onSubmit} className="cep-form">
                    <div className="cep-row">
                        <label htmlFor="name" className="cep-label">Назва *</label>
                        <input
                            id="name"
                            name="name"
                            required
                            value={form.name}
                            onChange={onChange}
                            className="cep-input"
                            placeholder="Назва події"
                        />
                    </div>

                    <div className="cep-row">
                        <label htmlFor="startAt" className="cep-label">Початок (дата/час) *</label>
                        <input
                            id="startAt"
                            name="startAt"
                            type="datetime-local"
                            required
                            value={form.startAt}
                            onChange={onChange}
                            className="cep-input"
                        />
                    </div>

                    <div className="cep-grid">
                        <div className="cep-row">
                            <label htmlFor="location" className="cep-label">Локація</label>
                            <input
                                id="location"
                                name="location"
                                value={form.location}
                                onChange={onChange}
                                className="cep-input"
                                placeholder="Місто, адреса або назва залу"
                            />
                        </div>
                        <div className="cep-row">
                            <label htmlFor="capacity" className="cep-label">Вмістимість</label>
                            <input
                                id="capacity"
                                name="capacity"
                                type="number"
                                min="0"
                                value={form.capacity}
                                onChange={onChange}
                                className="cep-input"
                                placeholder="Напр. 150"
                            />
                        </div>
                    </div>

                    <div className="cep-grid">
                        <div className="cep-row">
                            <label htmlFor="coverUrl" className="cep-label">Обкладинка (URL)</label>
                            <input
                                id="coverUrl"
                                name="coverUrl"
                                value={form.coverUrl}
                                onChange={onChange}
                                className="cep-input"
                                placeholder="https://…"
                            />
                        </div>
                        <div className="cep-row">
                            <label htmlFor="category" className="cep-label">Категорія (ENUM name)</label>
                            <input
                                id="category"
                                name="category"
                                value={form.category}
                                onChange={onChange}
                                className="cep-input"
                                placeholder="Напр. FOOTBALL"
                            />
                        </div>
                    </div>

                    <div className="cep-row">
                        <label htmlFor="tags" className="cep-label">Теги (через кому)</label>
                        <input
                            id="tags"
                            name="tags"
                            value={form.tags}
                            onChange={onChange}
                            className="cep-input"
                            placeholder="fitness, kids, outdoor"
                        />
                    </div>

                    <div className="cep-row">
                        <label htmlFor="description" className="cep-label">Опис</label>
                        <textarea
                            id="description"
                            name="description"
                            rows="4"
                            value={form.description}
                            onChange={onChange}
                            className="cep-input"
                            placeholder="Деталі події…"
                        />
                    </div>

                    <div className="cep-actions">
                        <button
                            type="button"
                            className="btn btn-ghost"
                            onClick={() => navigate(-1)}
                            disabled={submitting || deleting}
                        >
                            Скасувати
                        </button>
                        <button type="submit" className="btn btn-primary" disabled={submitting || deleting}>
                            {submitting ? "Збереження…" : "Зберегти зміни"}
                        </button>
                    </div>
                </form>
            </div>

            <style>{styles}</style>
        </div>
    );
}

const styles = `
.container{ max-width:1000px; margin:0 auto; padding:20px }
.toolbar{ display:flex; align-items:center; justify-content:space-between; margin-bottom:14px }
.page-title{ font-size:22px; font-weight:700 }
.toolbar__right{ display:flex; gap:10px }

.cep-card{ background:#0e1622; border:1px solid #ffffff14; border-radius:14px; padding:20px }
.cep-form{ display:flex; flex-direction:column; gap:14px }
.cep-grid{ display:grid; grid-template-columns:1fr 1fr; gap:14px }
.cep-row{ display:flex; flex-direction:column; gap:6px }
.cep-label{ font-weight:600; font-size:13px; color:#9fb2c7 }
.cep-input{ height:40px; padding:8px 12px; border-radius:10px; border:1px solid #ffffff22; background:#0f1826; color:#e8eef6 }
textarea.cep-input{ height:auto; resize:vertical; padding-top:10px }
.cep-actions{ display:flex; justify-content:flex-end; gap:10px; margin-top:6px }

.btn{ display:inline-flex; align-items:center; justify-content:center; height:36px; padding:0 14px; border-radius:10px; border:1px solid #ffffff22; background:#131a26; text-decoration:none; font-weight:600; font-size:14px; color:#e8eef6 }
.btn-primary{ background:#1a2740; border-color:#4c7fff66 }
.btn-ghost{ background:#0f1318; border-color:#ffffff1a }
.btn-danger{ background:#3a1518; border-color:#d54b57; color:#ffdadd }

.alert{ padding:12px; border-radius:10px }
.alert-danger{ background:#3b0f14; border:1px solid #a83a46; color:#ffd5d8 }

@media (max-width:760px){ .cep-grid{ grid-template-columns:1fr } }
`;
