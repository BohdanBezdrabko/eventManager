// ===============================
// File: src/pages/EventsPage.jsx
// Мета: показати ВСІ МАЙБУТНІ ІВЕНТИ (без фільтрів UI) і дати можливість перейти на деталі івенту.
// Алгоритм: GET /events → нормалізуємо → фільтруємо за майбутнім startAt → сортуємо зростаюче → рендеримо.
// ===============================
import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { getAllEvents } from "@/services/events.jsx";

// --------- Допоміжні утиліти ---------
const parseDate = (ev) => {
    // Підтримуємо кілька можливих полів дати початку
    const raw =
        ev?.startAt ??
        ev?.startDate ??
        ev?.startsAt ??
        ev?.dateFrom ??
        ev?.start_time ??
        ev?.start ??
        null;
    if (!raw) return null;
    const d = new Date(raw);
    return isNaN(d.getTime()) ? null : d;
};

const normalizeEvent = (ev) => {
    const id = ev?.id ?? ev?.eventId ?? ev?.event_id ?? null;
    if (id == null) return null; // без id — ігноруємо, щоб не було /events/undefined
    const name =
        ev?.name ??
        ev?.title ??
        ev?.eventName ??
        `Event #${id}`;
    const startsAt = parseDate(ev);
    const location =
        ev?.location ??
        ev?.place ??
        ev?.venue ??
        ev?.city ??
        null;
    return { id, name, startsAt, location, _raw: ev };
};

const fmtDate = (d) => (d ? new Date(d).toLocaleString() : "—");

// --------- Компонент сторінки ---------
export default function EventsPage() {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [events, setEvents] = useState([]);

    useEffect(() => {
        let cancelled = false;
        (async () => {
            setLoading(true);
            setError(null);
            try {
                const raw = await getAllEvents();
                const normalized = (Array.isArray(raw) ? raw : [])
                    .map(normalizeEvent)
                    .filter(Boolean);

                // Тільки майбутні (>= зараз)
                const now = new Date();
                const upcoming = normalized.filter((e) => !e.startsAt || e.startsAt >= now ? true : false)
                    // Якщо дата відсутня, можна або показувати внизу, або сховати.
                    // Тут ми показуємо навіть без дати, але нижче сортування помістить їх у кінець.
                ;

                // Сортування: за датою зростаюче; відсутні дати — внизу
                upcoming.sort((a, b) => {
                    const ta = a.startsAt ? a.startsAt.getTime() : Number.POSITIVE_INFINITY;
                    const tb = b.startsAt ? b.startsAt.getTime() : Number.POSITIVE_INFINITY;
                    return ta - tb;
                });

                if (!cancelled) setEvents(upcoming);
            } catch (e) {
                if (!cancelled) setError(e);
            } finally {
                if (!cancelled) setLoading(false);
            }
        })();
        return () => { cancelled = true; };
    }, []);

    const info = useMemo(() => ({
        total: events.length,
    }), [events]);

    return (
        <div className="ap-wrap">
            <div className="ap-top">
                <h1 className="ap-h1">Майбутні івенти</h1>
                <div className="ap-stats">
                    <span className="chip">Всього: {info.total}</span>
                </div>
            </div>

            {loading && <div className="muted">Завантаження…</div>}
            {error && (
                <div className="ap-alert">
                    <strong>Помилка:</strong> {error?.message || `HTTP ${error?.status || "?"}`}
                </div>
            )}

            {!loading && !error && (
                events.length === 0 ? (
                    <div className="ap-empty">Немає запланованих івентів.</div>
                ) : (
                    <div className="ap-card">
                        <table className="ap-table">
                            <thead>
                            <tr>
                                <th>Назва</th>
                                <th>Дата та час</th>
                                <th>Локація</th>
                                <th></th>
                            </tr>
                            </thead>
                            <tbody>
                            {events.map((e) => (
                                <tr key={e.id}>
                                    <td>{e.name}</td>
                                    <td className="nowrap">{fmtDate(e.startsAt)}</td>
                                    <td className="nowrap">{e.location ?? "—"}</td>
                                    <td className="nowrap">
                                        {/* Посилання на сторінку деталей івенту */}
                                        <Link className="ap-link" to={`/events/${encodeURIComponent(e.id)}`}>
                                            Перейти до деталей
                                        </Link>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )
            )}

            <style>{styles}</style>
        </div>
    );
}

const styles = `
.ap-wrap{max-width:900px;margin:0 auto;padding:16px}
.ap-top{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;gap:12px;flex-wrap:wrap}
.ap-h1{margin:0;font-size:24px}
.ap-stats{display:flex;gap:8px;flex-wrap:wrap}
.ap-card{background:#fff;border:1px solid #e5e7eb;border-radius:12px;overflow:hidden}
.ap-table{width:100%;border-collapse:collapse}
.ap-table th,.ap-table td{padding:10px 12px;border-bottom:1px solid #f3f4f6;text-align:left;vertical-align:top}
.ap-table th{font-weight:600;color:#374151;background:#f9fafb}
.ap-empty{padding:18px;text-align:center;color:#6b7280}
.ap-alert{margin:8px 0;padding:10px 12px;border-radius:10px;border:1px solid #fecdd3;background:#fff1f2;color:#b91c1c}
.chip{display:inline-flex;align-items:center;height:24px;padding:0 8px;border-radius:9999px;background:#f3f4f6;color:#111827;font-size:12px}
.ap-link{color:#2563eb}
.nowrap{white-space:nowrap}
`;
