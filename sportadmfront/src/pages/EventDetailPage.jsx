// src/pages/EventDetailPage.jsx
import { useEffect, useMemo, useState, useCallback } from "react";
import { useParams, Link } from "react-router-dom";
import {
    getEventById,
    getEventCreator,
    getTelegramSubscriptionCount,
} from "@/services/events.jsx";
import { listPosts, Audience, Channel, PostStatus } from "@/services/posts.jsx";

function fmt(dt) {
    return dt ? new Date(dt).toLocaleString() : "—";
}
function isValidHttpUrl(u) {
    try {
        const url = new URL(u);
        return url.protocol === "http:" || url.protocol === "https:";
    } catch {
        return false;
    }
}

export default function EventDetailPage() {
    const { id } = useParams();

    const [ev, setEv] = useState(null);
    const [creator, setCreator] = useState(null);
    const [tgSubs, setTgSubs] = useState(null);

    const [loading, setLoading] = useState(true);
    const [err, setErr] = useState("");

    const [posts, setPosts] = useState([]);
    const [postsLoading, setPostsLoading] = useState(false);
    const [postsErr, setPostsErr] = useState("");
    const [filters, setFilters] = useState({
        status: "",
        audience: "",
        channel: "",
        q: "",
    });

    const fetchEvent = useCallback(async () => {
        setLoading(true);
        setErr("");
        try {
            const e = await getEventById(id);
            setEv(e);
        } catch (e) {
            setErr(e?.message || "Не вдалося завантажити івент");
        } finally {
            setLoading(false);
        }
    }, [id]);

    const fetchCreatorSoft = useCallback(async () => {
        try {
            const cr = await getEventCreator(id);
            setCreator(cr);
        } catch (e) {
            // 404 або інша помилка — ігноруємо, не валимо сторінку
            setCreator(null);
        }
    }, [id]);

    const fetchTgCountSoft = useCallback(async () => {
        try {
            const x = await getTelegramSubscriptionCount(id);
            setTgSubs(x);
        } catch {
            setTgSubs(null);
        }
    }, [id]);

    const fetchPosts = useCallback(async () => {
        try {
            setPostsLoading(true);
            setPostsErr("");
            const data = await listPosts(id, {
                status: filters.status,
                audience: filters.audience,
                channel: filters.channel,
            });
            const arr =
                (Array.isArray(data) && data) ||
                data?.content ||
                data?.items ||
                data?.results ||
                data?.data ||
                data?.list ||
                [];
            const q = (filters.q || "").trim().toLowerCase();
            setPosts(
                q
                    ? arr.filter(
                        (p) =>
                            (p.title || "").toLowerCase().includes(q) ||
                            (p.body || "").toLowerCase().includes(q)
                    )
                    : arr
            );
        } catch (e) {
            setPostsErr(e?.message || "Не вдалося завантажити пости");
        } finally {
            setPostsLoading(false);
        }
    }, [id, filters]);

    useEffect(() => {
        fetchEvent().then(() => {
            // Після основного івенту — «мʼякі» запити
            fetchCreatorSoft();
            fetchTgCountSoft();
            fetchPosts();
        });
    }, [fetchEvent, fetchCreatorSoft, fetchTgCountSoft, fetchPosts]);

    const tags = useMemo(() => (Array.isArray(ev?.tags) ? ev.tags : []), [ev]);
    const bgStyle =
        ev?.coverUrl && isValidHttpUrl(ev.coverUrl)
            ? { backgroundImage: `url(${ev.coverUrl})` }
            : undefined;

    // універсальне ім'я автора з дефолтом
    const authorName = useMemo(
        () =>
            (creator &&
                (creator.username ||
                    creator.name ||
                    creator.displayName ||
                    creator.email)) ||
            "невідомий автор посту",
        [creator]
    );

    // >>> НОВЕ: знаходимо URL івенту та відображаємо його ТІЛЬКИ текстом
    const website = useMemo(() => {
        const raw =
            ev?.siteUrl ??
            ev?.url ??
            ev?.website ??
            ev?.link ??
            null;
        if (typeof raw !== "string") return null;
        const t = raw.trim();
        return t.length ? t : null;
    }, [ev]);

    return (
        <div className="container py-4">
            <style>{styles}</style>

            <div className="toolbar">
                <h1 className="page-title">{ev?.name || "Подія"}</h1>
                <div className="toolbar__right">
                    <Link className="btn btn-outline-primary" to={`/events/${id}/edit`}>
                        Змінити подію
                    </Link>
                    <Link className="btn btn-outline-primary" to={`/events/${id}/posts/create`}>
                        + Новий пост
                    </Link>
                    <Link className="btn btn-ghost" to="/events">
                        ← До списку
                    </Link>
                </div>
            </div>

            {loading ? (
                <div className="muted">Завантаження…</div>
            ) : err ? (
                <div className="alert alert-danger">{err}</div>
            ) : !ev ? (
                <div className="muted">Івент не знайдено</div>
            ) : (
                <>
                    {/* HERO */}
                    <section className="hero">
                        <div className="hero__cover" style={bgStyle} />
                        <div className="hero__body">
                            <div className="hero__row">
                                <span className="chip">{fmt(ev.startAt)}</span>
                                {ev.location && <span className="chip chip--ghost">{ev.location}</span>}
                                {ev.category && <span className="chip chip--ghost">{ev.category}</span>}
                                {ev.capacity ? (
                                    <span className="chip chip--ghost">{ev.capacity} місць</span>
                                ) : null}
                                <span className="chip chip--ghost">Автор: {authorName}</span>
                                {typeof tgSubs?.count === "number" && (
                                    <span className="chip chip--ghost">TG: {tgSubs.count}</span>
                                )}
                            </div>

                            {tags.length > 0 && (
                                <div className="tags">
                                    {tags.map((t, i) => (
                                        <span key={i} className="tag">
                                            #{t}
                                        </span>
                                    ))}
                                </div>
                            )}

                            {ev.description && <p className="hero__desc">{ev.description}</p>}
                        </div>
                    </section>

                    <div className="grid">
                        {/* LEFT: Posts */}
                        <section className="panel">
                            <div className="panel__head">
                                <h3 className="panel__title">Пости</h3>
                                <div className="filters">
                                    <div className="filters__item">
                                        <span className="filters__label">Статус</span>
                                        <select
                                            className="select"
                                            value={filters.status}
                                            onChange={(e) => setFilters((s) => ({ ...s, status: e.target.value }))}
                                        >
                                            <option value="">всі</option>
                                            <option value={PostStatus.DRAFT}>чернетки</option>
                                            <option value={PostStatus.SCHEDULED}>заплановані</option>
                                            <option value={PostStatus.PUBLISHED}>опубліковані</option>
                                            <option value={PostStatus.FAILED}>помилки</option>
                                            <option value={PostStatus.CANCELLED}>скасовані</option>
                                        </select>
                                    </div>
                                    <div className="filters__item">
                                        <span className="filters__label">Аудиторія</span>
                                        <select
                                            className="select"
                                            value={filters.audience}
                                            onChange={(e) => setFilters((s) => ({ ...s, audience: e.target.value }))}
                                        >
                                            <option value="">всі</option>
                                            <option value={Audience.PUBLIC}>публічна</option>
                                            <option value={Audience.SUBSCRIBERS}>підписники</option>
                                        </select>
                                    </div>
                                    <div className="filters__item">
                                        <span className="filters__label">Канал</span>
                                        <select
                                            className="select"
                                            value={filters.channel}
                                            onChange={(e) => setFilters((s) => ({ ...s, channel: e.target.value }))}
                                        >
                                            <option value="">всі</option>
                                            <option value={Channel.TELEGRAM}>Telegram</option>
                                            <option value={Channel.EMAIL}>Email</option>
                                        </select>
                                    </div>
                                    <div className="filters__item" style={{ flex: 1 }}>
                                        <input
                                            className="input"
                                            placeholder="Пошук у постах…"
                                            value={filters.q}
                                            onChange={(e) => setFilters((s) => ({ ...s, q: e.target.value }))}
                                        />
                                    </div>
                                </div>
                            </div>

                            {postsLoading ? (
                                <div className="muted">Завантаження…</div>
                            ) : postsErr ? (
                                <div className="alert alert-danger">{postsErr}</div>
                            ) : posts.length === 0 ? (
                                <div className="muted">Поки що немає постів.</div>
                            ) : (
                                <ul className="list">
                                    {posts.map((p) => (
                                        <li key={p.id} className="card card--row">
                                            <div>
                                                <h4 className="card__title">
                                                    <Link to={`/events/${id}/posts/${p.id}`}>{p.title || "Пост"}</Link>
                                                </h4>
                                                <div className="card__meta">
                                                    <span className="chip">{fmt(p.publishAt)}</span>
                                                    <span className="chip chip--ghost">{p.status}</span>
                                                    <span className="chip chip--ghost">{p.audience}</span>
                                                    <span className="chip chip--ghost">{p.channel}</span>
                                                </div>
                                            </div>
                                            <div className="card__actions">
                                                <Link className="btn btn-sm" to={`/events/${id}/posts/${p.id}`}>
                                                    Відкрити
                                                </Link>
                                            </div>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </section>

                        {/* RIGHT: Info */}
                        <aside className="panel panel--side">
                            <h3 className="panel__title">Інформація</h3>
                            <div className="info">
                                <div className="info__row">
                                    <div className="info__label">Початок</div>
                                    <div className="info__value">{fmt(ev.startAt)}</div>
                                </div>
                                <div className="info__row">
                                    <div className="info__label">Локація</div>
                                    <div className="info__value">{ev.location || "—"}</div>
                                </div>
                                <div className="info__row">
                                    <div className="info__label">Категорія</div>
                                    <div className="info__value">{ev.category || "—"}</div>
                                </div>
                                <div className="info__row">
                                    <div className="info__label">Вмістимість</div>
                                    <div className="info__value">{ev.capacity ?? "—"}</div>
                                </div>
                                <div className="info__row">
                                    <div className="info__label">Автор</div>
                                    <div className="info__value">{authorName}</div>
                                </div>

                                {/* >>> НОВЕ: URL івенту як простий текст, без <a> і без переходів */}
                                {website ? (
                                    <div className="info__row">
                                        <div className="info__label">Сайт</div>
                                        <div className="info__value">
                                            <code className="url-plain">{website}</code>
                                        </div>
                                    </div>
                                ) : null}

                                <div className="divider" />
                                <div className="info__row">
                                    <div className="info__label">Пости</div>
                                    <div className="info__value">{posts.length}</div>
                                </div>
                                <div className="info__row">
                                    <div className="info__label">TG підписки</div>
                                    <div className="info__value">
                                        {typeof tgSubs?.count === "number" ? tgSubs.count : "—"}
                                    </div>
                                </div>
                            </div>
                        </aside>
                    </div>
                </>
            )}
        </div>
    );
}

const styles = `
:root{
  --bg:#0b1118; --panel:#0e1622; --panel-2:#111b29; --text:#e8eef6; --muted:#9fb2c7; --accent:#5b8cff; --accent-2:#4c7fff66;
}
.container{ max-width:1100px; margin:0 auto; padding:24px }
.toolbar{ display:flex; align-items:center; justify-content:space-between; gap:12px; margin-bottom:16px }
.page-title{ margin:0; font-size:22px }
.toolbar__right{ display:flex; gap:8px; flex-wrap:wrap }
.btn{ padding:8px 12px; border-radius:10px; border:1px solid #ffffff22; background:#121c2b; text-decoration:none; cursor:pointer; color:var(--text) }
.btn-sm{ padding:6px 10px; font-size:12px }
.btn-outline-primary{ border-color:#2f88ff66 }
.btn-ghost{ background:transparent }
.btn-ghost:hover{ background:#ffffff18 }
.input{ height:36px; padding:0 12px; border-radius:10px; border:1px solid #ffffff22; background:#0f1826; color:var(--text) }
.select{ background:var(--panel-2); border:1px solid #ffffff1a; color:var(--text); padding:8px 10px; border-radius:10px; outline:none }

.hero{ display:grid; grid-template-columns: 320px 1fr; gap:14px; margin-bottom:16px }
.hero__cover{ width:100%; height:220px; background:#0f1826 center/cover no-repeat; border-radius:14px; border:1px solid #ffffff14 }
.hero__body{ background:var(--panel); border:1px solid #ffffff14; border-radius:14px; padding:14px }
.hero__row{ display:flex; gap:8px; flex-wrap:wrap; margin-bottom:10px }
.hero__desc{ margin:0; color:var(--text) }
.tags{ display:flex; gap:8px; flex-wrap:wrap; margin-bottom:10px }
.tag{ font-size:12px; color:#adc2da }

.grid{ display:grid; grid-template-columns: 1fr 320px; gap:16px }
.panel{ background:var(--panel); border:1px solid #ffffff14; border-radius:14px; padding:14px }
.panel--side{ background:var(--panel-2) }
.panel__head{ display:flex; align-items:center; justify-content:space-between; gap:10px; margin-bottom:10px }
.panel__title{ margin:0; font-size:16px; color:var(--text) }

.list{ list-style:none; padding:0; margin:0; display:flex; flex-direction:column; gap:10px }
.card{ background:var(--panel-2); border:1px solid #ffffff14; border-radius:14px; padding:14px }
.card--row{ display:flex; align-items:flex-start; gap:12px; justify-content:space-between }
.card__title{ margin:0 0 4px 0; font-size:16px }
.card__meta{ display:flex; gap:8px; flex-wrap:wrap }
.card__actions{ display:flex; gap:8px }

.chip{ display:inline-flex; align-items:center; gap:6px; font-size:12px; padding:4px 8px; border-radius:999px; background:#ffffff10; border:1px solid #ffffff12; color:var(--text) }
.chip--ghost{ color:var(--muted) }

.info{ display:flex; flex-direction:column; gap:8px }
.info__row{ display:grid; grid-template-columns: 120px 1fr; gap:8px; align-items:start }
.info__label{ color:var(--muted); font-size:12px }
.info__value{ color:var(--text) }

.url-plain{ background:#0f1826; border:1px solid #ffffff1a; padding:2px 6px; border-radius:6px; font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,"Liberation Mono",monospace; user-select:text; word-break:break-all }

.divider{ height:1px; background:#ffffff14; margin:12px 0 }
.muted{ color:var(--muted) }
.alert{ padding:12px; border-radius:10px }
.alert-danger{ background:#3b0f14; border:1px solid #a83a46; color:#ffd5d8 }

@media (max-width:980px){
  .hero{ grid-template-columns:1fr }
  .grid{ grid-template-columns:1fr }
}
`;
