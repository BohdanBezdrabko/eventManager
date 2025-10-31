// src/pages/EventDetailPage.jsx
import { useEffect, useMemo, useState, useCallback } from "react";
import { useParams, Link } from "react-router-dom";
import { getEventById, getTelegramSubscriptionCount } from "@/services/events.jsx";
import { listPosts, Audience, Channel, PostStatus } from "@/services/posts.jsx";

function fmt(dt) { return dt ? new Date(dt).toLocaleString() : "—"; }

export default function EventDetailPage() {
    const { id } = useParams();
    const [ev, setEv] = useState(null);
    const [tgSubs, setTgSubs] = useState(null);
    const [loading, setLoading] = useState(true);
    const [err, setErr] = useState("");

    const [postFilters, setPostFilters] = useState({ status: "", audience: "", channel: "", q: "" });
    const [posts, setPosts] = useState([]);
    const [postsLoading, setPostsLoading] = useState(false);

    useEffect(() => {
        (async () => {
            try {
                setLoading(true);
                const d = await getEventById(id);
                setEv(d);
                try {
                    const c = await getTelegramSubscriptionCount(id);
                    const val = typeof c === "object" && c !== null && "count" in c ? c.count : c;
                    setTgSubs(Number(val ?? 0));
                } catch { setTgSubs(0); }
                setErr("");
            } catch (e) {
                setErr(e?.message || "Помилка завантаження івента.");
            } finally {
                setLoading(false);
            }
        })();
    }, [id]);

    const statuses = useMemo(() => Object.values(PostStatus), []);
    const channels = useMemo(() => Object.values(Channel), []);
    const audiences = useMemo(() => Object.values(Audience), []);

    const loadPosts = useCallback(async () => {
        try {
            setPostsLoading(true);
            const res = await listPosts(id, {
                status: postFilters.status || undefined,
                audience: postFilters.audience || undefined,
                channel: postFilters.channel || undefined,
                q: postFilters.q || undefined,
            });
            const items = Array.isArray(res) ? res : (res.items || res.content || []);
            setPosts(items);
        } catch (e) {
            setErr(e?.message || "Помилка завантаження постів.");
        } finally {
            setPostsLoading(false);
        }
    }, [id, postFilters]);

    useEffect(() => { if (id) loadPosts(); }, [id, loadPosts]);

    return (
        <div className="container py-4">
            <style>{styles}</style>

            {loading ? (
                <div>Завантаження…</div>
            ) : err ? (
                <div className="panel alert alert-danger">{err}</div>
            ) : !ev ? (
                <div className="panel muted">Івент не знайдено.</div>
            ) : (
                <>
                    <div className="panel">
                        <div className="card card--row">
                            <div className="card__main">
                                <h3 className="card__title">{ev.name}</h3>
                                <div className="card__meta">
                                    <span className="chip">{fmt(ev.startAt)}</span>
                                    <span className="chip chip--ghost">{ev.location || "—"}</span>
                                    {tgSubs !== null && <span className="chip">TG підписок: {tgSubs}</span>}
                                </div>
                            </div>
                            <div className="card__aside">
                                <div className="buttons">
                                    {ev?.coverUrl && <a className="btn btn-ghost" href={ev.coverUrl} target="_blank" rel="noreferrer">Посилання</a>}
                                </div>
                            </div>
                        </div>

                        <div className="divider" />
                        <div className="toolbar">
                            <h4 className="page-title">Пости івента</h4>
                            <div className="toolbar__right">
                                <Link className="btn btn-outline-primary" to={`/events/${id}/posts/new`}>+ Новий пост</Link>
                            </div>
                        </div>

                        {/* Фільтри */}
                        <div className="filters">
                            <div className="filters__item">
                                <span className="filters__label">Статус</span>
                                <select className="select" value={postFilters.status} onChange={(e)=>setPostFilters(f=>({...f,status:e.target.value}))}>
                                    <option value="">—</option>
                                    {statuses.map(s=><option key={s} value={s}>{s}</option>)}
                                </select>
                            </div>
                            <div className="filters__item">
                                <span className="filters__label">Канал</span>
                                <select className="select" value={postFilters.channel} onChange={(e)=>setPostFilters(f=>({...f,channel:e.target.value}))}>
                                    <option value="">—</option>
                                    {channels.map(s=><option key={s} value={s}>{s}</option>)}
                                </select>
                            </div>
                            <div className="filters__item">
                                <span className="filters__label">Аудиторія</span>
                                <select className="select" value={postFilters.audience} onChange={(e)=>setPostFilters(f=>({...f,audience:e.target.value}))}>
                                    <option value="">—</option>
                                    {audiences.map(s=><option key={s} value={s}>{s}</option>)}
                                </select>
                            </div>
                        </div>

                        {postsLoading ? (
                            <div className="muted">Завантаження постів…</div>
                        ) : posts.length === 0 ? (
                            <div className="muted">Пости відсутні</div>
                        ) : (
                            <div className="list-group">
                                {posts.map((p) => (
                                    <div className="card card--row" key={p.id}>
                                        <div className="card__main">
                                            <h4 className="card__title">
                                                <Link to={`/events/${id}/posts/${p.id}`}>{p.title || `Пост #${p.id}`}</Link>
                                            </h4>
                                            <div className="card__meta">
                                                <span className="chip">{p.channel}</span>
                                                <span className="chip">{p.audience}</span>
                                                <span className="chip chip--ghost">{p.status}</span>
                                                {p.publishAt && <span className="chip">{fmt(p.publishAt)}</span>}
                                            </div>
                                        </div>
                                        <div className="card__aside">
                                            <div className="buttons">
                                                <Link className="btn btn-outline-primary btn-sm" to={`/events/${id}/posts/${p.id}`}>Деталі</Link>
                                                <Link className="btn btn-ghost btn-sm" to={`/events/${id}/posts/${p.id}/edit`}>Редагувати</Link>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    {err && <div className="panel alert alert-danger mt-3">{err}</div>}
                </>
            )}
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
.toolbar{ display:flex; align-items:center; justify-content:space-between; gap:12px; margin-bottom:10px }
.page-title{ margin:0; font-size:20px }
.panel{ background:var(--panel); border:1px solid #ffffff19; border-radius:16px; padding:16px }
.card{ background:var(--panel-2); border:1px solid #ffffff14; border-radius:14px; padding:14px }
.card--row{ display:flex; align-items:flex-start; gap:12px; justify-content:space-between }
.card__title{ margin:0 0 6px; font-size:18px }
.card__meta{ display:flex; gap:8px; flex-wrap:wrap }
.chip{ display:inline-flex; align-items:center; gap:6px; font-size:12px; padding:6px 10px; border-radius:999px; background:#ffffff08; border:1px solid #ffffff12; color:var(--text) }
.chip--ghost{ color:var(--muted) }
.buttons{ display:flex; gap:8px; flex-wrap:wrap }
.btn{ padding:8px 12px; border-radius:999px; border:1px solid #ffffff22; background:#ffffff0d; color:var(--text); text-decoration:none; cursor:pointer }
.btn-sm{ padding:6px 10px; font-size:12px }
.btn-outline-primary{ border-color:#2f88ff66 }
.btn-ghost{ background:transparent }
.btn-ghost:hover{ background:#ffffff18 }
.select{ background:var(--panel-2); border:1px solid #ffffff1a; color:var(--text); padding:8px 10px; border-radius:10px; outline:none }
.filters{ display:flex; gap:10px; flex-wrap:wrap; margin-bottom:12px }
.filters__item{ display:flex; align-items:center; gap:6px }
.filters__label{ color:var(--muted); font-size:12px }
.divider{ height:1px; background:#ffffff14; margin:12px 0 }
.muted{ color:var(--muted) }
.alert{ padding:12px; border-radius:10px }
.alert-danger{ background:#3b0f14; border:1px solid #a83a46; color:#ffd5d8 }
@media (max-width:720px){ .toolbar{ flex-direction:column; align-items:stretch } }
`;
