// src/pages/EditPostPage.jsx
import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
    Audience,
    Channel,
    PostStatus,
    getPost,
    updatePost,
    setPostStatus,
} from "@/services/posts.jsx";

/** ISO -> value для <input type="datetime-local"> (локальний) */
function toDatetimeLocal(iso) {
    if (!iso) return "";
    const d = new Date(iso);
    // корекція на таймзону — щоб у полі було локальне значення
    d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
    return d.toISOString().slice(0, 16);
}

/** value з <input type="datetime-local"> -> справжній ISO (UTC) */
function fromDatetimeLocal(localValue) {
    if (!localValue) return null;
    const d = new Date(localValue);
    // повертаємо ISO у UTC
    d.setMinutes(d.getMinutes() + d.getTimezoneOffset());
    return d.toISOString();
}

export default function EditPostPage() {
    // !!! ВАЖЛИВО: якщо у роуті параметр називається :id — мапимо його на eventId
    // Якщо ти перейменуєш маршрут на :eventId — можеш повернути { eventId, postId }
    const { id: eventId, postId } = useParams();
    const navigate = useNavigate();

    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [err, setErr] = useState("");

    // Потрібно, щоб розуміти, чи змінився статус
    const [originalStatus, setOriginalStatus] = useState(PostStatus.DRAFT);

    const [form, setForm] = useState({
        title: "",
        body: "",
        publishAt: "",
        audience: Audience.PUBLIC,
        channel: Channel.TELEGRAM,
        status: PostStatus.DRAFT,
        telegramChatId: "",
    });

    const audienceOptions = useMemo(
        () => [
            { value: Audience.PUBLIC, label: "Публічна" },
            { value: Audience.PRIVATE, label: "Приватна" },
        ],
        []
    );

    const channelOptions = useMemo(
        () => [{ value: Channel.TELEGRAM, label: "Telegram" }],
        []
    );

    const statusOptions = useMemo(
        () => Object.values(PostStatus).map((s) => ({ value: s, label: s })),
        []
    );

    useEffect(() => {
        let alive = true;

        // Підстраховка: не робимо запити, якщо параметри відсутні
        if (!eventId || !postId) {
            setErr("Невірний маршрут: відсутні eventId або postId.");
            setLoading(false);
            return () => {
                alive = false;
            };
        }

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

                setOriginalStatus(p.status ?? PostStatus.DRAFT);
            } catch (e) {
                if (!alive) return;
                setErr(e?.message || "Не вдалося завантажити пост.");
            } finally {
                if (!alive) return;
                setLoading(false);
            }
        })();

        return () => {
            alive = false;
        };
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
            // Валідація для запланованих постів
            if (form.status === PostStatus.SCHEDULED && !form.publishAt) {
                throw new Error("Для статусу SCHEDULED потрібно вказати дату публікації.");
            }

            // 1) Оновлюємо метадані поста БЕЗ поля status
            const payload = {
                ...form,
                publishAt: fromDatetimeLocal(form.publishAt),
            };
            delete payload.status; // статус міняємо окремим ендпоінтом

            await updatePost(eventId, postId, payload);

            // 2) Якщо статус змінився — окремий PATCH /status
            if (form.status !== originalStatus) {
                await setPostStatus(eventId, postId, { status: form.status });
            }

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
                    {eventId && postId && (
                        <Link
                            className="btn btn-outline-primary"
                            to={`/events/${eventId}/posts/${postId}`}
                        >
                            До деталей
                        </Link>
                    )}
                    {eventId && (
                        <Link className="btn btn-ghost" to={`/events/${eventId}`}>
                            До івенту
                        </Link>
                    )}
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
                                placeholder="Назва поста"
                            />
                        </div>

                        <div className="field">
                            <label className="label">Текст</label>
                            <textarea
                                className="textarea"
                                name="body"
                                value={form.body}
                                onChange={onChange}
                                placeholder="Основний текст"
                                rows={8}
                            />
                        </div>

                        <div className="grid-2">
                            <div className="field">
                                <label className="label">Опублікувати о</label>
                                <input
                                    type="datetime-local"
                                    className="input"
                                    name="publishAt"
                                    value={form.publishAt}
                                    onChange={onChange}
                                    // підказка: зробимо обов'язковим лише для SCHEDULED
                                    required={form.status === PostStatus.SCHEDULED}
                                />
                            </div>

                            <div className="field">
                                <label className="label">Аудиторія</label>
                                <select
                                    className="select"
                                    name="audience"
                                    value={form.audience}
                                    onChange={onChange}
                                >
                                    {audienceOptions.map((o) => (
                                        <option key={o.value} value={o.value}>
                                            {o.label}
                                        </option>
                                    ))}
                                </select>
                            </div>
                        </div>

                        <div className="grid-2">
                            <div className="field">
                                <label className="label">Канал</label>
                                <select
                                    className="select"
                                    name="channel"
                                    value={form.channel}
                                    onChange={onChange}
                                >
                                    {channelOptions.map((o) => (
                                        <option key={o.value} value={o.value}>
                                            {o.label}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div className="field">
                                <label className="label">Статус</label>
                                <select
                                    className="select"
                                    name="status"
                                    value={form.status}
                                    onChange={onChange}
                                >
                                    {statusOptions.map((o) => (
                                        <option key={o.value} value={o.value}>
                                            {o.label}
                                        </option>
                                    ))}
                                </select>
                            </div>
                        </div>

                        <div className="grid-2">
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
                            {eventId && postId && (
                                <Link
                                    to={`/events/${eventId}/posts/${postId}`}
                                    className="btn btn-ghost"
                                >
                                    Скасувати
                                </Link>
                            )}
                        </div>
                    </form>
                )}
            </div>
        </div>
    );
}

const styles = `
.container{ max-width:960px; margin:0 auto }
.py-4{ padding:24px 16px }
.toolbar{ display:flex; align-items:center; justify-content:space-between; gap:16px; margin-bottom:16px }
.page-title{ font-size:22px; font-weight:700; margin:0 }
.toolbar__right{ display:flex; gap:8px; align-items:center }
.panel{
  background:linear-gradient(180deg,#0b1220,#0b1220 60%,#0d1627);
  border:1px solid #192338;
  border-radius:12px;
  padding:16px;
}
.muted{ color:#a3aac2 }
.form{ display:flex; flex-direction:column; gap:12px }
.field{ display:flex; flex-direction:column; gap:6px }
.label{ font-size:13px; color:#c6cbe0 }
.input, .select, .textarea{
  background:#0f1729; border:1px solid #212b43; color:#e8ecff;
  padding:10px 12px; border-radius:10px; outline:none;
}
.input:focus, .select:focus, .textarea:focus{ border-color:#2f76ff }
.textarea{ resize:vertical }
.grid-2{ display:grid; grid-template-columns:1fr 1fr; gap:12px }
@media (max-width: 720px){ .grid-2{ grid-template-columns:1fr } }

.actions{ display:flex; gap:8px; align-items:center; flex-wrap:wrap; margin-top:4px }
.btn{
  display:inline-flex; align-items:center; justify-content:center; gap:8px; padding:8px 12px;
  border-radius:10px; border:1px solid #ffffff22; cursor:pointer; text-decoration:none;
  font-weight:600; font-size:14px; transition:transform .06s ease, border-color .15s ease; user-select:none; color:var(--text)
}
.btn:active{ transform:translateY(1px) }
.btn-outline-primary{ background:transparent; border-color:#2f76ff; color:#e8ecff }
.btn-outline-primary:hover{ background:#0f1a2a }
.btn-ghost{ background:#ffffff10; border-color:#ffffff20; color:#e8ecff }
.btn-ghost:hover{ background:#ffffff18 }
.alert{ padding:12px; border-radius:10px }
.alert-danger{ background:#3b0f14; border:1px solid #a83a46; color:#ffd5d8 }
`;
