// src/pages/EditPostPage.jsx
import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { Audience, Channel, PostStatus, getPost, updatePost } from "@/services/posts.jsx";

/** ISO -> value для <input type="datetime-local"> */
function toDatetimeLocal(iso) {
    if (!iso) return "";
    const d = new Date(iso);
    d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
    return d.toISOString().slice(0, 16);
}

/** value з <input type="datetime-local"> -> справжній ISO (UTC) */
function fromDatetimeLocal(localValue) {
    if (!localValue) return null;
    const [date, time] = localValue.split("T");
    const [y, m, d] = date.split("-").map(Number);
    const [hh, mm] = time.split(":").map(Number);
    const local = new Date(y, (m - 1), d, hh, mm, 0);
    return new Date(local.getTime() - local.getTimezoneOffset() * 60000).toISOString();
}

export default function EditPostPage() {
    const { id: eventId, postId } = useParams();
    const navigate = useNavigate();

    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [err, setErr] = useState("");

    const [form, setForm] = useState({
        title: "",
        body: "",
        publishAt: "",
        audience: Audience.PUBLIC,
        channel: Channel.TELEGRAM,
        status: PostStatus.DRAFT,
        telegramChatId: "",
    });

    // Підписи для селектів у єдиному стилі
    const audienceOptions = useMemo(
        () => [
            { value: Audience.PUBLIC, label: "Публічна" },
            { value: Audience.SUBSCRIBERS, label: "Підписники" },
        ],
        []
    );

    const channelOptions = useMemo(
        () => [
            { value: Channel.TELEGRAM, label: "Telegram" },
        ],
        []
    );

    const statusOptions = useMemo(
        () => Object.values(PostStatus).map((s) => ({ value: s, label: s })),
        []
    );

    useEffect(() => {
        let alive = true;
        (async () => {
            try {
                setLoading(true);
                const p = await getPost(eventId, postId);
                if (!alive) return;
                setForm({
                    title: p.title ?? "",
                    body: p.body ?? "",
                    publishAt: toDatetimeLocal(p.publishAt),
                    audience: p.audience ?? Audience.PUBLIC,
                    channel: p.channel ?? Channel.TELEGRAM,
                    status: p.status ?? PostStatus.DRAFT,
                    telegramChatId: p.telegramChatId ?? "",
                });
            } catch (e) {
                if (!alive) return;
                setErr(e?.message || "Не вдалося завантажити пост.");
            } finally {
                if (alive) setLoading(false);
            }
        })();
        return () => { alive = false; };
    }, [eventId, postId]);

    function onChange(e) {
        const { name, value } = e.target;
        setForm((prev) => ({ ...prev, [name]: value }));
    }

    async function onSubmit(e) {
        e.preventDefault();
        setSaving(true);
        setErr("");
        try {
            const payload = {
                ...form,
                publishAt: fromDatetimeLocal(form.publishAt),
                capacity: undefined, // нічого зайвого
            };
            await updatePost(eventId, postId, payload);
            navigate(`/events/${eventId}/posts/${postId}`);
        } catch (e2) {
            setErr(e2?.message || "Помилка збереження.");
        } finally {
            setSaving(false);
        }
    }

    return (
        <div className="container py-4">
            <style>{styles}</style>

            <div className="toolbar">
                <h1 className="page-title">Редагувати пост</h1>
                <div className="toolbar__right">
                    <Link className="btn btn-outline-primary" to={`/events/${eventId}/posts/${postId}`}>До деталей</Link>
                    <Link className="btn btn-ghost" to={`/events/${eventId}`}>До івенту</Link>
                </div>
            </div>

            <div className="panel">
                {err && <div className="alert alert-danger mb-2">{err}</div>}

                {loading ? (
                    <div className="muted">Завантаження…</div>
                ) : (
                    <form onSubmit={onSubmit} className="form">
                        <div className="field">
                            <label className="label">Заголовок</label>
                            <input
                                type="text"
                                className="input"
                                name="title"
                                value={form.title}
                                onChange={onChange}
                                placeholder="Напр. «Нагадування за 1 годину до старту»"
                                required
                            />
                        </div>

                        <div className="field">
                            <label className="label">Тіло</label>
                            <textarea
                                className="textarea"
                                name="body"
                                rows={8}
                                value={form.body}
                                onChange={onChange}
                                placeholder="Текст повідомлення"
                            />
                        </div>

                        <div className="grid-3">
                            <div className="field">
                                <label className="label">Опублікувати о</label>
                                <input
                                    type="datetime-local"
                                    className="input"
                                    name="publishAt"
                                    value={form.publishAt}
                                    onChange={onChange}
                                />
                            </div>

                            <div className="field">
                                <label className="label">Аудиторія</label>
                                <select className="select" name="audience" value={form.audience} onChange={onChange}>
                                    {audienceOptions.map((o) => (
                                        <option key={o.value} value={o.value}>{o.label}</option>
                                    ))}
                                </select>
                            </div>

                            <div className="field">
                                <label className="label">Канал</label>
                                <select className="select" name="channel" value={form.channel} onChange={onChange}>
                                    {channelOptions.map((o) => (
                                        <option key={o.value} value={o.value}>{o.label}</option>
                                    ))}
                                </select>
                            </div>
                        </div>

                        <div className="grid-2">
                            <div className="field">
                                <label className="label">Статус</label>
                                <select className="select" name="status" value={form.status} onChange={onChange}>
                                    {statusOptions.map((o) => (
                                        <option key={o.value} value={o.value}>{o.label}</option>
                                    ))}
                                </select>
                            </div>

                            <div className="field">
                                <label className="label">Telegram Chat ID</label>
                                <input
                                    className="input"
                                    name="telegramChatId"
                                    value={form.telegramChatId}
                                    onChange={onChange}
                                    placeholder="за потреби"
                                />
                            </div>
                        </div>

                        <div className="actions">
                            <button disabled={saving} className="btn btn-outline-primary" type="submit">
                                {saving ? "Збереження…" : "Зберегти"}
                            </button>
                            <Link to={`/events/${eventId}/posts/${postId}`} className="btn btn-ghost">Скасувати</Link>
                        </div>
                    </form>
                )}
            </div>
        </div>
    );
}

const styles = `
:root{
  --bg:#0b121a; --panel:#0f1318; --panel-2:#121922; --line:#1f2a37;
  --text:#e9f0f6; --muted:#9fb3c8; --accent:#2f88ff; --accent-2:#2473da;
  --radius:14px;
}
.container{ color:var(--text) }
.page-title{ margin:0 }
.toolbar{ display:flex; justify-content:space-between; align-items:center; gap:12px; margin-bottom:16px }
.toolbar__right{ display:flex; gap:8px; flex-wrap:wrap }
.panel{ background:var(--panel); border:1px solid #ffffff19; border-radius:16px; padding:16px }
.form{ display:flex; flex-direction:column; gap:12px }
.field{ display:flex; flex-direction:column; gap:6px }
.label{ color:var(--muted); font-size:12px }
.input, .textarea, .select{
  background:var(--panel-2); border:1px solid #ffffff1a; color:var(--text);
  padding:10px 12px; border-radius:10px; outline:none
}
.textarea{ min-height:120px; resize:vertical }
.input::placeholder, .textarea::placeholder{ color:var(--muted) }
.grid-3{ display:grid; grid-template-columns:1fr 1fr 1fr; gap:12px }
.grid-2{ display:grid; grid-template-columns:1fr 1fr; gap:12px }
@media (max-width:900px){ .grid-3{ grid-template-columns:1fr } .grid-2{ grid-template-columns:1fr } }
.actions{ display:flex; gap:8px; align-items:center; flex-wrap:wrap; margin-top:4px }
.btn{
  display:inline-flex; align-items:center; justify-content:center; gap:8px; padding:8px 12px;
  border-radius:10px; border:1px solid #ffffff22; cursor:pointer; text-decoration:none;
  font-weight:600; font-size:14px; transition:transform .06s ease, background .15s ease, border-color .15s ease; user-select:none; color:var(--text)
}
.btn:active{ transform:translateY(1px) }
.btn-outline-primary{ background:transparent; border-color:var(--accent-2) }
.btn-outline-primary:hover{ background:#0f1a2a }
.btn-ghost{ background:#ffffff10; border-color:#ffffff20 }
.btn-ghost:hover{ background:#ffffff18 }
.alert{ padding:12px; border-radius:10px }
.alert-danger{ background:#3b0f14; border:1px solid #a83a46; color:#ffd5d8 }
`;
