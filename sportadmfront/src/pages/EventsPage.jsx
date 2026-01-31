import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { getAllEvents } from "@/services/events.jsx";
import "./events.css";

const parseDate = (ev) => {
    const raw =
        ev?.startAt ??
        ev?.startDate ??
        ev?.startsAt ??
        ev?.dateFrom ??
        ev?.start_time ??
        ev?.start ??
        null;

    if (raw == null) return null;
    // Accept numeric timestamps and ISO strings
    const val = typeof raw === "number" || /^\d+$/.test(String(raw)) ? Number(raw) : String(raw);
    const d = new Date(val);
    return isNaN(d.getTime()) ? null : d;
};

const normalizeEvent = (ev) => {
    const id = ev?.id ?? ev?.eventId ?? ev?.event_id ?? null;
    if (id == null) return null;

    const name = ev?.name ?? ev?.title ?? ev?.eventName ?? `Event #${id}`;
    const startsAt = parseDate(ev);
    const location = ev?.location ?? ev?.place ?? ev?.venue ?? ev?.city ?? null;

    return { id, name, startsAt, location, _raw: ev };
};

const fmtDate = (d) =>
    d ? new Date(d).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" }) : "—";

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
                const normalized = (Array.isArray(raw) ? raw : []).map(normalizeEvent).filter(Boolean);
                const now = new Date();
                const upcoming = normalized.filter((e) => (!e.startsAt ? true : e.startsAt >= now));
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
        return () => {
            cancelled = true;
        };
    }, []);

    const info = useMemo(() => ({ total: events.length }), [events]);

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
                <div className="ap-alert" role="alert">
                    <strong>Помилка:</strong> {error?.message ?? JSON.stringify(error)}
                </div>
            )}

            {!loading && !error && (
                events.length === 0 ? (
                    <div className="ap-empty">Немає запланованих івентів.</div>
                ) : (
                    <div className="ap-card">
                        <table className="ap-table" role="grid">
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
                                    <td data-label="Назва">{e.name}</td>
                                    <td className="nowrap" data-label="Дата та час">{fmtDate(e.startsAt)}</td>
                                    <td className="nowrap" data-label="Локація">{e.location ?? "—"}</td>
                                    <td className="nowrap" data-label="">
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
        </div>
    );
}