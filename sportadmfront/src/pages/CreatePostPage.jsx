import { useMemo, useState, useEffect } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { Audience, Channel, PostStatus, createPost } from "@/services/posts.jsx";
import { http } from "@/utils/fetchWrapper.jsx";

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
    const { id: eventId } = useParams();
    const navigate = useNavigate();

    // Початковий стан за замовчуванням
    const [form, setForm] = useState({
        title: "",
        body: "",
        publishAt: toDatetimeLocalNow(),
        audience: Audience.SUBSCRIBERS,
        channel: Channel.WHATSAPP,
        status: PostStatus.DRAFT,
        telegramChatId: "",
    });

    const [submitting, setSubmitting] = useState(false);
    const [err, setErr] = useState("");
    const [templateLoading, setTemplateLoading] = useState(true);

    // ✅ НОВЕ: Завантажити шаблон поста з бекенда при завантаженні сторінки
    useEffect(() => {
        (async () => {
            try {
                const template = await http.get(`/events/${encodeURIComponent(eventId)}/posts/new`);
                if (template) {
                    setForm((prev) => ({
                        ...prev,
                        publishAt: template.publishAt || prev.publishAt,
                        audience: template.audience || prev.audience,
                        channel: template.channel || prev.channel,
                        status: template.status || prev.status,
                        telegramChatId: template.telegramChatId || "",
                    }));
                }
            } catch (ex) {
                console.warn("Failed to load post template:", ex?.message);
                // Використовуємо дефолти, помилка не критична
            } finally {
                setTemplateLoading(false);
            }
        })();
    }, [eventId]);

    const canSubmit = useMemo(
        () => form.title.trim() && form.body.trim(),
        [form.title, form.body]
    );


    const onChange = (e) => {
        const { name, value } = e.target;
        setForm((s) => ({ ...s, [name]: value }));
    };


    const onSubmit = async (e) => {
        e.preventDefault();
        try {
            setSubmitting(true);
            setErr("");

            // Валідація форми
            if (!form.title.trim()) {
                throw new Error("Заголовок обов'язковий");
            }
            if (!form.body.trim()) {
                throw new Error("Текст обов'язковий");
            }
            if (!form.channel) {
                throw new Error("Канал обов'язковий");
            }
            if (!form.audience) {
                throw new Error("Аудиторія обов'язкова");
            }

            console.log("Creating post with payload:", {
                title: form.title,
                body: form.body.substring(0, 100) + "...",
                publishAt: form.publishAt,
                audience: form.audience,
                channel: form.channel,
                status: form.status,
            });

            const payload = {
                title: form.title.trim(),
                body: form.body,
                publishAt: fromDatetimeLocal(form.publishAt),
                audience: form.audience,
                channel: form.channel,
                status: form.status,
            };


            // Telegram-специфічні поля (legacy)
            if (form.channel === Channel.TELEGRAM && form.telegramChatId?.trim()) {
                payload.telegramChatId = form.telegramChatId.trim();
            }

            const created = await createPost(eventId, payload);
            console.log("Post created successfully:", created);

            alert("✅ Пост створено успішно!");
            navigate(`/events/${eventId}/posts/${created.id}`);
        } catch (ex) {
            console.error("Post creation error:", ex);
            const errorMsg = ex?.message || "Не вдалось створити пост";
            setErr(errorMsg);
            alert(`❌ ${errorMsg}`);
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

                {templateLoading ? (
                    <div className="muted" style={{ padding: "20px", textAlign: "center" }}>⏳ Завантаження форми...</div>
                ) : (
                    <form onSubmit={onSubmit} className="cep-form">
                        {/* Заголовок */}
                        <div className="cep-row">
                            <label className="cep-label">Заголовок</label>
                            <input
                                className="cep-input"
                                name="title"
                                value={form.title}
                                onChange={onChange}
                                required
                                placeholder="Назва поста"
                            />
                        </div>

                        {/* Текст */}
                        <div className="cep-row">
                            <label className="cep-label">Текст</label>
                            <textarea
                                className="cep-input"
                                rows={8}
                                name="body"
                                value={form.body}
                                onChange={onChange}
                                required
                                placeholder="Основний текст…"
                            />
                        </div>

                        {/* Дата/час та Канал */}
                        <div className="cep-grid">
                            <div className="cep-row">
                                <label className="cep-label">Дата/час публікації</label>
                                <input
                                    className="cep-input"
                                    type="datetime-local"
                                    name="publishAt"
                                    value={form.publishAt}
                                    onChange={onChange}
                                />
                            </div>
                            <div className="cep-row">
                                <label className="cep-label">Канал</label>
                                <select
                                    className="cep-input"
                                    name="channel"
                                    value={form.channel}
                                    onChange={onChange}
                                >
                                    <option value={Channel.TELEGRAM}>Telegram</option>
                                    <option value={Channel.WHATSAPP}>WhatsApp</option>
                                </select>
                            </div>
                        </div>

                        {/* Аудиторія та Статус */}
                        <div className="cep-grid">
                            <div className="cep-row">
                                <label className="cep-label">Аудиторія</label>
                                <select
                                    className="cep-input"
                                    name="audience"
                                    value={form.audience}
                                    onChange={onChange}
                                >
                                <option value={Audience.PUBLIC}>Публічна</option>
                                <option value={Audience.SUBSCRIBERS}>Підписники</option>
                                </select>
                            </div>
                            <div className="cep-row">
                                <label className="cep-label">Статус</label>
                                <select
                                    className="cep-input"
                                    name="status"
                                    value={form.status}
                                    onChange={onChange}
                                >
                                    <option value={PostStatus.DRAFT}>Чернетка</option>
                                    <option value={PostStatus.SCHEDULED}>Заплановано</option>
                                </select>
                            </div>
                        </div>


                        {/* Telegram-специфічні поля */}
                        {form.channel === Channel.TELEGRAM && (
                            <>
                                <div className="cep-divider">
                                    <h4 className="cep-divider__title">⚙️ Параметри Telegram</h4>
                            </div>
                            <div className="cep-row">
                                <label className="cep-label">Telegram Chat ID (необов.)</label>
                                <input
                                    className="cep-input"
                                    name="telegramChatId"
                                    value={form.telegramChatId}
                                    onChange={onChange}
                                    placeholder="Напр. -1001234567890"
                                />
                            </div>
                        </>
                    )}

                    {/* Кнопки действия */}
                    <div className="cep-actions">
                        <Link to={`/events/${eventId}`} className="btn btn-ghost">
                            Скасувати
                        </Link>
                        <button
                            className="btn btn-primary"
                            disabled={submitting || !canSubmit}
                        >
                            {submitting ? "Створення…" : "Створити пост"}
                        </button>
                    </div>
                    </form>
                )}
            </div>
        </div>
    );
}

const cpStyles = `
.container {
    max-width: 900px;
    margin: 0 auto;
    padding: 24px;
}
.toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
    gap: 12px;
}
.page-title {
    margin: 0;
    font-size: 22px;
    font-weight: 700;
}
.cep-card {
    background: #0e1622;
    border: 1px solid #ffffff14;
    border-radius: 14px;
    padding: 20px;
}
.cep-form {
    display: flex;
    flex-direction: column;
    gap: 14px;
}
.cep-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 14px;
}
.cep-row {
    display: flex;
    flex-direction: column;
    gap: 6px;
}
.cep-label {
    font-weight: 600;
    font-size: 13px;
    color: #9fb2c7;
    display: flex;
    align-items: center;
    gap: 6px;
}
.cep-help-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    border: 1px solid #ffffff22;
    background: transparent;
    color: #9fb2c7;
    cursor: pointer;
    font-size: 12px;
    padding: 0;
}
.cep-help-btn:hover {
    background: #ffffff11;
}
.cep-help-box {
    background: #0f1826;
    border: 1px solid #4c7fff44;
    border-radius: 10px;
    padding: 12px;
    font-size: 12px;
    color: #c5d5e6;
    margin-top: 6px;
}
.cep-help-box ol {
    margin: 8px 0;
    padding-left: 20px;
}
.cep-help-box li {
    margin: 4px 0;
}
.cep-help-box em {
    color: #9fb2c7;
}
.cep-input {
    height: 40px;
    padding: 8px 12px;
    border-radius: 10px;
    border: 1px solid #ffffff22;
    background: #0f1826;
    color: #e8eef6;
    font-family: inherit;
    font-size: 14px;
}
.cep-input:focus {
    outline: none;
    border-color: #4c7fff;
    box-shadow: 0 0 0 2px #4c7fff22;
}
textarea.cep-input {
    height: auto;
    resize: vertical;
    padding-top: 10px;
}
.cep-checkbox-label {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    font-weight: 500;
    color: #e8eef6;
}
.cep-checkbox-label input[type="checkbox"] {
    width: 18px;
    height: 18px;
    cursor: pointer;
    accent-color: #4c7fff;
}
.cep-hint {
    font-size: 12px;
    color: #9fb2c7;
    margin: 6px 0 0 0;
}
.cep-divider {
    border-top: 1px solid #ffffff14;
    padding-top: 14px;
    margin-top: 10px;
}
.cep-divider__title {
    font-size: 13px;
    color: #9fb2c7;
    font-weight: 600;
    margin: 0;
}
.cep-actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    margin-top: 6px;
}
.btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 8px 12px;
    border-radius: 10px;
    border: 1px solid #ffffff22;
    cursor: pointer;
    text-decoration: none;
    font-weight: 600;
    font-size: 14px;
    color: #e8eef6;
    transition: all 0.2s;
}
.btn:hover {
    border-color: #ffffff44;
}
.btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}
.btn-primary {
    background: #1a2740;
    border-color: #4c7fff66;
}
.btn-primary:hover:not(:disabled) {
    background: #1e2f4f;
    border-color: #4c7fff;
}
.btn-ghost {
    background: #0f1318;
    border-color: #ffffff1a;
}
.btn-ghost:hover {
    background: #131820;
}
.alert {
    padding: 12px;
    border-radius: 10px;
}
.alert-danger {
    background: #3b0f14;
    border: 1px solid #a83a46;
    color: #ffd5d8;
}
@media (max-width: 760px) {
    .cep-grid {
        grid-template-columns: 1fr;
    }
}
`;
