import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
    deletePost,
    getPost,
    publishNow,
    setPostStatus,
    PostStatus,
    Channel,
    Audience,
} from "@/services/posts.jsx";

function fmt(dt) { return dt ? new Date(dt).toLocaleString() : "—"; }

function StatusBadge({ status }) {
    return <span className="chip">{status || "—"}</span>;
}

export default function PostDetailPage() {
    const { id: eventId, postId } = useParams();
    const navigate = useNavigate();

    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [busy, setBusy] = useState(false);
    const [err, setErr] = useState("");

    const load = async () => {
        try {
            setLoading(true);
            const d = await getPost(eventId, postId);
            setData(d);
        } catch (e) {
            setErr(e?.message || "Не вдалося завантажити пост.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        load();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [eventId, postId]);

    const doPublishNow = async () => {
        try {
            setBusy(true);
            await publishNow(eventId, postId);
            await load();
        } catch (e) {
            setErr(e?.message || "Не вдалося опублікувати.");
        } finally {
            setBusy(false);
        }
    };

    const markStatus = async (status) => {
        try {
            setBusy(true);
            await setPostStatus(eventId, postId, status);
            await load();
        } catch (e) {
            setErr(e?.message || "Не вдалося оновити статус.");
        } finally {
            setBusy(false);
        }
    };

    const handleDelete = async () => {
        if (!window.confirm("Видалити пост?")) return;
        try {
            setBusy(true);
            await deletePost(eventId, postId);
            navigate(`/events/${eventId}`);
        } catch (e) {
            setErr(e?.message || "Не вдалося видалити.");
        } finally {
            setBusy(false);
        }
    };

    const meta = useMemo(() => ([
        ["Статус", <StatusBadge key="st" status={data?.status} />],
        ["Опублікувати о", fmt(data?.publishAt)],
        ["Створено", fmt(data?.createdAt)],
        ["Оновлено", fmt(data?.updatedAt)],
        ["Канал", data?.channel || "—"],
        ["Аудиторія", data?.audience || "—"],
        ["Telegram Chat ID", data?.telegramChatId ?? "—"],
    ]), [data]);

    return (
        <div className="container py-4">
            <style>{styles}</style>

            <div className="toolbar">
                <h1 className="page-title">Пост #{postId}</h1>
                <div className="toolbar__right">
                    <Link className="btn btn-outline-primary" to={`/events/${eventId}`}>До івенту</Link>
                    <Link className="btn btn-ghost" to={`/events/${eventId}/posts/${postId}/edit`}>Редагувати</Link>
                </div>
            </div>

            <div className="panel">
                {loading ? (
                    <div className="muted">Завантаження…</div>
                ) : err ? (
                    <div className="alert alert-danger">{err}</div>
                ) : !data ? (
                    <div className="muted">Пост не знайдено.</div>
                ) : (
                    <>
                        <div className="card">
                            <div className="card__main">
                                <h3 className="card__title">{data.title || `Пост #${postId}`}</h3>
                                <div className="card__meta">
                                    <span className="chip">{data.channel || Channel.TELEGRAM}</span>
                                    <span className="chip">{data.audience || Audience.PUBLIC}</span>
                                    <span className="chip chip--ghost">{data.status || PostStatus.DRAFT}</span>
                                    {data.publishAt && <span className="chip">{fmt(data.publishAt)}</span>}
                                </div>
                            </div>
                        </div>

                        <div className="grid-2">
                            <div className="panel soft">
                                <div className="muted small mb-1">Контент</div>
                                <pre className="pre">{data.body || "—"}</pre>
                            </div>

                            <div className="panel soft">
                                <div className="muted small mb-2">Метадані</div>
                                <dl className="meta">
                                    {meta.map(([k, v]) => (
                                        <div className="meta__row" key={k}>
                                            <dt>{k}</dt>
                                            <dd>{v}</dd>
                                        </div>
                                    ))}
                                </dl>
                            </div>
                        </div>

                        <div className="actions">
                            <button disabled={busy} onClick={doPublishNow} className="btn btn-outline-primary">Опублікувати зараз</button>
                            <div className="spacer" />
                            <button disabled={busy} onClick={() => markStatus(PostStatus.DRAFT)} className="btn btn-ghost">DRAFT</button>
                            <button disabled={busy} onClick={() => markStatus(PostStatus.SCHEDULED)} className="btn btn-ghost">SCHEDULED</button>
                            <button disabled={busy} onClick={() => markStatus(PostStatus.PUBLISHED)} className="btn btn-ghost">PUBLISHED</button>
                            <button disabled={busy} onClick={() => markStatus(PostStatus.FAILED)} className="btn btn-ghost">FAILED</button>
                            <button disabled={busy} onClick={() => markStatus(PostStatus.CANCELLED)} className="btn btn-ghost">CANCELLED</button>
                            <div className="spacer" />
                            <button disabled={busy} onClick={handleDelete} className="btn btn-ghost danger">Видалити</button>
                        </div>
                    </>
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
.panel{ background:var(--panel); border:1px solid #ffffff19; border-radius:16px; padding:16px; margin-bottom:12px }
.panel.soft{ background:var(--panel-2); border-color:#ffffff14 }
.card{ background:var(--panel-2); border:1px solid #ffffff14; border-radius:var(--radius); padding:14px; margin-bottom:12px }
.card__title{ margin:0 0 4px 0; font-size:18px }
.card__meta{ display:flex; gap:8px; flex-wrap:wrap }
.chip{ display:inline-flex; align-items:center; gap:6px; font-size:12px; padding:6px 8px; border-radius:999px; background:#ffffff08; border:1px solid #ffffff12; color:var(--text) }
.chip--ghost{ color:var(--muted) }
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
.btn.danger{ border-color:#a83a46; background:#3b0f14 }
.grid-2{ display:grid; grid-template-columns:1fr 1fr; gap:12px }
@media (max-width:900px){ .grid-2{ grid-template-columns:1fr } }
.pre{ white-space:pre-wrap; padding:12px; border-radius:10px; background:#0b1119; border:1px solid #ffffff14 }
.meta{ margin:0 }
.meta__row{ display:grid; grid-template-columns:160px 1fr; gap:8px; padding:6px 0; border-bottom:1px dashed #ffffff14 }
.meta__row:last-child{ border-bottom:none }
.meta dt{ color:var(--muted) }
.actions{ display:flex; gap:8px; align-items:center; flex-wrap:wrap; margin-top:8px }
.spacer{ flex:1 }
.muted{ color:var(--muted) }
.alert{ padding:12px; border-radius:10px }
.alert-danger{ background:#3b0f14; border:1px solid #a83a46; color:#ffd5d8 }
`;
