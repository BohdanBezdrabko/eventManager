// src/pages/CreatePostPage.jsx
import { useState, useMemo } from "react";
import { useNavigate, useParams, Link } from "react-router-dom";
import { createPost, Audience, Channel, PostStatus } from "@/services/posts.jsx";

function toDatetimeLocalNow() {
    const d = new Date();
    d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
    return d.toISOString().slice(0, 16);
}
function fromDatetimeLocal(local) {
    if (!local) return null;
    const d = new Date(local);
    // перетворюємо в справжній ISO (UTC) для бекенду
    return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString();
}

export default function CreatePostPage() {
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

    const channelHint = useMemo(() => {
        if (form.channel === Channel.TELEGRAM) {
            return "ID чату Telegram (необов’язково). Якщо порожньо — використається дефолтний канал.";
        }
        if (form.channel === Channel.EMAIL) {
            return "E-mail аудиторія формується автоматично.";
        }
        return "";
    }, [form.channel]);

    function onChange(e) {
        const { name, value } = e.target;
        setForm((prev) => ({ ...prev, [name]: value }));
    }

    async function onSubmit(e) {
        e.preventDefault();
        setErr("");
        setSubmitting(true);
        try {
            const payload = {
                title: form.title?.trim(),
                body: form.body?.trim(),
                publishAt: fromDatetimeLocal(form.publishAt),
                audience: form.audience,
                channel: form.channel,
                status: form.status,
                telegramChatId: form.telegramChatId?.trim() || null,
            };

            // невелика валідація на фронті
            if (!payload.title) throw new Error("Вкажіть заголовок поста.");
            if (!payload.body) throw new Error("Заповніть текст поста.");
            if (!payload.publishAt) throw new Error("Вкажіть час публікації.");

            const res = await createPost(eventId, payload);
            const newId = res?.id ?? res?.postId ?? res?.post_id;
            if (newId) navigate(`/events/${eventId}/posts/${newId}`);
            else navigate(`/events/${eventId}`);
        } catch (e2) {
            setErr(e2?.message || "Не вдалося створити пост.");
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <div className="container py-4">
            <div className="row justify-content-center">
                <div className="col-lg-9">
                    <div className="card shadow-sm">
                        <div className="card-body">
                            <div className="d-flex justify-content-between align-items-center mb-3">
                                <h3 className="m-0">Створити пост</h3>
                                <Link to={`/events/${eventId}`} className="btn btn-outline-secondary">
                                    ← Назад до івента
                                </Link>
                            </div>

                            {err && (
                                <div className="alert alert-danger" role="alert">
                                    {err}
                                </div>
                            )}

                            <form onSubmit={onSubmit} className="row g-3">
                                <div className="col-12">
                                    <label className="form-label">Заголовок</label>
                                    <input
                                        type="text"
                                        name="title"
                                        className="form-control"
                                        placeholder="Вкажіть заголовок"
                                        value={form.title}
                                        onChange={onChange}
                                        required
                                    />
                                </div>

                                <div className="col-12">
                                    <label className="form-label">Текст</label>
                                    <textarea
                                        name="body"
                                        className="form-control"
                                        rows={8}
                                        placeholder="Основний текст поста"
                                        value={form.body}
                                        onChange={onChange}
                                        required
                                    />
                                </div>

                                <div className="col-sm-6">
                                    <label className="form-label">Час публікації</label>
                                    <input
                                        type="datetime-local"
                                        name="publishAt"
                                        className="form-control"
                                        value={form.publishAt}
                                        onChange={onChange}
                                        required
                                    />
                                </div>

                                <div className="col-sm-6">
                                    <label className="form-label">Статус</label>
                                    <select
                                        name="status"
                                        className="form-select"
                                        value={form.status}
                                        onChange={onChange}
                                    >
                                        <option value={PostStatus.DRAFT}>Чернетка</option>
                                        <option value={PostStatus.SCHEDULED}>Запланований</option>
                                        <option value={PostStatus.PUBLISHED}>Опублікований</option>
                                        <option value={PostStatus.FAILED}>Помилка</option>
                                        <option value={PostStatus.CANCELLED}>Скасований</option>
                                    </select>
                                </div>

                                <div className="col-sm-6">
                                    <label className="form-label">Аудиторія</label>
                                    <select
                                        name="audience"
                                        className="form-select"
                                        value={form.audience}
                                        onChange={onChange}
                                    >
                                        <option value={Audience.PUBLIC}>Публічно</option>
                                        <option value={Audience.SUBSCRIBERS}>Підписники</option>
                                    </select>
                                </div>

                                <div className="col-sm-6">
                                    <label className="form-label">Канал</label>
                                    <select
                                        name="channel"
                                        className="form-select"
                                        value={form.channel}
                                        onChange={onChange}
                                    >
                                        <option value={Channel.TELEGRAM}>Telegram</option>
                                        <option value={Channel.EMAIL}>E-mail</option>
                                    </select>
                                </div>

                                {form.channel === Channel.TELEGRAM && (
                                    <div className="col-12">
                                        <label className="form-label">Telegram Chat ID (опц.)</label>
                                        <input
                                            type="text"
                                            name="telegramChatId"
                                            className="form-control"
                                            placeholder="Напр.: -1001234567890"
                                            value={form.telegramChatId}
                                            onChange={onChange}
                                        />
                                        {channelHint && <div className="form-text">{channelHint}</div>}
                                    </div>
                                )}

                                <div className="col-12 d-flex gap-2 justify-content-end mt-1">
                                    <Link className="btn btn-outline-secondary" to={`/events/${eventId}`}>
                                        Скасувати
                                    </Link>
                                    <button className="btn btn-primary" disabled={submitting}>
                                        {submitting ? "Створення…" : "Створити пост"}
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>

                </div>
            </div>
        </div>
    );
}
