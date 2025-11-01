// ===============================
// File: src/pages/EventDetailPage.jsx
// ===============================
import { useEffect, useState } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import { getEventById } from "@/services/events.jsx";

/** Перевіряємо, чи рядок схожий на повний http(s) URL */
function asHttpUrl(u) {
    if (!u || typeof u !== "string") return undefined;
    try {
        const url = new URL(u);
        if (!["http:", "https:"].includes(url.protocol)) return undefined;
        return url.toString();
    } catch {
        return undefined;
    }
}

export default function EventDetailPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const { user } = useAuth();

    const [event, setEvent] = useState(null);
    const [loading, setLoading] = useState(true);
    const [err, setErr] = useState("");

    useEffect(() => {
        let cancelled = false;

        async function load() {
            try {
                setLoading(true);
                setErr("");
                const data = await getEventById(id);
                if (cancelled) return;

                // Нормалізація: безпечні дефолти, щоб не падало, якщо чогось немає
                const normalized = {
                    id: data?.id ?? data?._id ?? id,
                    name: data?.name ?? data?.title ?? "Без назви",
                    description: data?.description ?? data?.shortDescription ?? "",
                    startAt: data?.startAt ?? data?.date ?? null,
                    location: data?.location ?? data?.place ?? "",
                    author:
                        data?.author ??
                        data?.createdBy ??
                        data?.user ??
                        null, // може бути null — нижче відобразимо "невідомий"
                    // Раніше це було обкладинкою; тепер лишаємо як гіперпосилання в деталях:
                    link:
                        asHttpUrl(data?.coverUrl) ||
                        asHttpUrl(data?.link) ||
                        asHttpUrl(data?.url) ||
                        undefined,
                    category: data?.category ?? "",
                    tags: Array.isArray(data?.tags) ? data.tags : [],
                };

                setEvent(normalized);
            } catch (e) {
                if (cancelled) return;
                setErr(e?.message || "Не вдалося завантажити подію");
            } finally {
                if (!cancelled) setLoading(false);
            }
        }

        load();
        return () => {
            cancelled = true;
        };
    }, [id]);

    const authorName =
        (event?.author &&
            (event.author.name ||
                event.author.username ||
                event.author.email)) ||
        "невідомий";

    return (
        <div className="container py-4">
            <style>{pageStyles}</style>

            <div className="header">
                <button className="btn" onClick={() => navigate(-1)}>
                    ← Назад
                </button>
                <div className="spacer" />
                <Link className="btn" to="/events">
                    Усі події
                </Link>
                {user && (
                    <Link className="btn btn--primary" to={`/events/${id}/edit`}>
                        Редагувати
                    </Link>
                )}
            </div>

            {loading ? (
                <div className="muted">Завантаження…</div>
            ) : err ? (
                <div className="alert alert--error">Помилка: {err}</div>
            ) : !event ? (
                <div className="alert">Подію не знайдено.</div>
            ) : (
                <article className="card">
                    <header className="card__header">
                        <h1 className="title">{event.name}</h1>
                        <div className="meta">
              <span className="chip">
                {event.startAt
                    ? new Date(event.startAt).toLocaleString()
                    : "Дата не вказана"}
              </span>
                            {event.location ? (
                                <span className="chip chip--ghost">{event.location}</span>
                            ) : null}
                            {event.category ? (
                                <span className="chip chip--ghost">{event.category}</span>
                            ) : null}
                        </div>
                    </header>

                    {/* ВАЖЛИВО: НІЯКИХ ЗОБРАЖЕНЬ. Жодних <img> або backgroundImage */}
                    {/* Якщо у події був coverUrl/інший URL — показуємо як ГІПЕРПОСИЛАННЯ */}
                    {event.link ? (
                        <section className="block">
                            <div className="block__title">Посилання</div>
                            <p className="block__content">
                                <a
                                    href={event.link}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                >
                                    {event.link}
                                </a>
                            </p>
                        </section>
                    ) : null}

                    {Array.isArray(event.tags) && event.tags.length > 0 ? (
                        <section className="block">
                            <div className="block__title">Теги</div>
                            <p className="block__content">
                                {event.tags.map((t) => (
                                    <span key={String(t)} className="chip chip--ghost">
                    {String(t)}
                  </span>
                                ))}
                            </p>
                        </section>
                    ) : null}

                    <section className="block">
                        <div className="block__title">Опис</div>
                        {event.description ? (
                            <p className="block__content prewrap">{event.description}</p>
                        ) : (
                            <p className="block__content muted">Опис відсутній.</p>
                        )}
                    </section>

                    <footer className="block">
                        <div className="block__title">Автор</div>
                        <p className="block__content">
                            {/* Якщо автора немає — не кидаємо помилку, показуємо “невідомий” */}
                            {authorName}
                        </p>
                    </footer>
                </article>
            )}
        </div>
    );
}

const pageStyles = `
.container{max-width:960px;margin:0 auto;padding:24px 16px}
.header{display:flex;align-items:center;gap:8px;margin-bottom:16px}
.spacer{flex:1}
.btn{padding:8px 12px;border:1px solid #e5e7eb;border-radius:10px;background:#fff;cursor:pointer;text-decoration:none;color:#111827}
.btn--primary{background:#111827;color:#fff;border-color:#111827}
.muted{color:#6b7280}
.alert{padding:12px;border-radius:10px;background:#f3f4f6;border:1px solid #e5e7eb}
.alert--error{background:#fff1f2;border-color:#fecdd3;color:#b91c1c}
.card{background:#fff;border:1px solid #e5e7eb;border-radius:16px;overflow:hidden}
.card__header{padding:16px 16px 0 16px}
.title{margin:0 0 8px 0;font-size:22px;line-height:1.3}
.meta{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:12px}
.chip{display:inline-flex;align-items:center;padding:6px 10px;border-radius:999px;background:#f3f4f6;border:1px solid #e5e7eb;font-size:12px;color:#111827}
.chip--ghost{background:#fff}
.block{padding:12px 16px;border-top:1px solid #f1f5f9}
.block__title{font-weight:600;margin-bottom:6px}
.block__content{margin:0}
.prewrap{white-space:pre-wrap}
a{word-break:break-all}
`;
