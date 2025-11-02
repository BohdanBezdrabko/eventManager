// ===============================
// File: src/pages/CreatePostPage.jsx
// ===============================
import { useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { Audience, Channel, PostStatus, createPost } from "@/services/posts.jsx";

function toDatetimeLocalNow() {
    const d = new Date();
    d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
    return d.toISOString().slice(0, 16);
}
function fromDatetimeLocal(local) {
    if (!local) return null;
    const d = new Date(local);
    return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString();
}

export default function CreatePostPage() {
    // ✅ виправлено: беремо :id з роутера і називаємо його eventId
    const { id: eventId } = useParams();
    const navigate = useNavigate();

    const [form, setForm] = useState({
        title: "",
        body: "",
        publishAt: toDatetimeLocalNow(),
        audience: Audience.PUBLIC,
        channel: Channel.TELEGRAM,
        status: PostStatus.DRAFT,
        telegramChatId: "",
    });
    const [submitting, setSubmitting] = useState(false);
    const [err, setErr] = useState("");

    const canSubmit = useMemo(() => form.title.trim() && form.body.trim(), [form.title, form.body]);

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
                title: form.title.trim(),
                body: form.body,
                publishAt: fromDatetimeLocal(form.publishAt),
                audience: form.audience,
                channel: form.channel,
                status: form.status,
                telegramChatId: form.telegramChatId?.trim() || null,
            };
            const created = await createPost(eventId, payload);
            navigate(`/events/${eventId}/posts/${created.id}`);
        } catch (ex) {
            console.error(ex);
            setErr(ex?.message || "Не вдалося створити пост");
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="container py-4">
            <style>{cpStyles}</style>

            <div className="toolbar">
                <h1 className="page-title">Новий пост</h1>
                <div className="toolbar__right">
                    <Link to={`/events/${eventId}`} className="btn btn-ghost">← До івенту</Link>
                </div>
            </div>

            <div className="cep-card">
                {err && <div className="alert alert-danger">{err}</div>}

                <form onSubmit={onSubmit} className="cep-form">
                    <div className="cep-row">
                        <label className="cep-label">Заголовок</label>
                        <input className="cep-input" name="title" value={form.title} onChange={onChange} required placeholder="Назва поста" />
                    </div>

                    <div className="cep-row">
                        <label className="cep-label">Текст</label>
                        <textarea className="cep-input" rows={8} name="body" value={form.body} onChange={onChange} required placeholder="Основний текст…" />
                    </div>

                    <div className="cep-grid">
                        <div className="cep-row">
                            <label className="cep-label">Дата/час публікації</label>
                            <input className="cep-input" type="datetime-local" name="publishAt" value={form.publishAt} onChange={onChange} />
                        </div>
                        <div className="cep-row">
                            <label className="cep-label">Канал</label>
                            <select className="cep-input" name="channel" value={form.channel} onChange={onChange}>
                                <option value={Channel.TELEGRAM}>Telegram</option>
                                <option value={Channel.EMAIL}>Email</option>
                            </select>
                        </div>
                    </div>

                    <div className="cep-grid">
                        <div className="cep-row">
                            <label className="cep-label">Аудиторія</label>
                            <select className="cep-input" name="audience" value={form.audience} onChange={onChange}>
                                <option value={Audience.PUBLIC}>Публічна</option>
                                <option value={Audience.SUBSCRIBERS}>Підписники</option>
                            </select>
                        </div>
                        <div className="cep-row">
                            <label className="cep-label">Статус</label>
                            <select className="cep-input" name="status" value={form.status} onChange={onChange}>
                                <option value={PostStatus.DRAFT}>Чернетка</option>
                                <option value={PostStatus.SCHEDULED}>Заплановано</option>
                            </select>
                        </div>
                    </div>

                    <div className="cep-row">
                        <label className="cep-label">Telegram chat ID (необов.)</label>
                        <input className="cep-input" name="telegramChatId" value={form.telegramChatId} onChange={onChange} placeholder="Напр. -1001234567890" />
                    </div>

                    <div className="cep-actions">
                        <Link to={`/events/${eventId}`} className="btn btn-ghost">Скасувати</Link>
                        <button className="btn btn-primary" disabled={submitting || !canSubmit}>
                            {submitting ? "Створення…" : "Створити пост"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

const cpStyles = `
.container{ max-width:900px; margin:0 auto }
.toolbar{ display:flex; align-items:center; justify-content:space-between; margin-bottom:16px }
.page-title{ font-size:20px; font-weight:700 }
.cep-card{ background:#0b1220; border:1px solid #ffffff1a; border-radius:14px; padding:16px }
.cep-form{ display:flex; flex-direction:column; gap:14px }
.cep-grid{ display:grid; grid-template-columns:1fr 1fr; gap:14px }
.cep-row{ display:flex; flex-direction:column; gap:6px }
.cep-label{ font-weight:600; font-size:13px; color:#9fb2c7 }
.cep-input{ height:40px; padding:8px 12px; border-radius:10px; border:1px solid #ffffff22; background:#0f1826; color:#e8eef6 }
textarea.cep-input{ height:auto; resize:vertical; padding-top:10px }
.cep-actions{ display:flex; justify-content:flex-end; gap:10px; margin-top:6px }
.btn{ display:inline-flex; align-items:center; justify-content:center; gap:8px; height:40px; padding:0 14px; border-radius:10px; border:1px solid #ffffff22; background:#0f1826; text-decoration:none; font-weight:600; font-size:14px; color:#e8eef6 }
.btn-primary{ background:#1a2740; border-color:#4c7fff66 }
.btn-ghost{ background:#0f1318; border-color:#ffffff1a }
.alert{ padding:12px; border-radius:10px }
.alert-danger{ background:#3b0f14; border:1px solid #a83a46; color:#ffd5d8 }
@media (max-width:760px){ .cep-grid{ grid-template-columns:1fr } }
`;
