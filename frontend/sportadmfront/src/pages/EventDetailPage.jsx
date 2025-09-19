// src/pages/EventDetailPage.jsx
import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { getEventById } from "@/services/events";
import { Calendar, MapPin, Users, ArrowLeft } from "lucide-react";

export default function EventDetailPage() {
    const { id } = useParams();
    const [event, setEvent] = useState(null);
    const [loading, setLoading] = useState(true);
    const [err, setErr] = useState(null);

    useEffect(() => {
        let cancelled = false;
        (async () => {
            try {
                const data = await getEventById(id);
                if (!cancelled) setEvent(data);
            } catch (e) {
                if (!cancelled) setErr(e.message || "Помилка");
            } finally {
                if (!cancelled) setLoading(false);
            }
        })();
        return () => { cancelled = true; };
    }, [id]);

    if (loading) return <div className="container py-4">Завантаження…</div>;
    if (err) return <div className="container py-4 text-danger">Помилка: {String(err)}</div>;
    if (!event) return <div className="container py-4">Івент не знайдено</div>;

    return (
        <div className="container py-4">
            <Link to="/events" className="btn btn-light mb-3">
                <ArrowLeft size={16} /> Повернутися
            </Link>

            <div className="card shadow-sm">
                <div className="card-body">
                    <h3 className="card-title">{event.name}</h3>
                    <div className="text-muted mb-3 d-flex gap-3 flex-wrap">
            <span className="d-inline-flex align-items-center gap-1">
              <Calendar size={16} /> {formatDate(event.date)}
            </span>
                        <span className="d-inline-flex align-items-center gap-1">
              <MapPin size={16} /> {event.location || "—"}
            </span>
                        {"capacity" in event || "registeredCount" in event ? (
                            <span className="d-inline-flex align-items-center gap-1">
                <Users size={16} /> {event.registeredCount ?? 0}{event.capacity ? ` / ${event.capacity}` : ""}
              </span>
                        ) : null}
                    </div>

                    {event.cover && (
                        <img src={event.cover} alt="" className="img-fluid rounded mb-3" />
                    )}

                    <p className="mb-0">{event.description || "Опис відсутній."}</p>
                </div>
            </div>
        </div>
    );
}

function formatDate(iso) {
    if (!iso) return "-";
    const d = new Date(iso);
    if (Number.isNaN(d)) return iso;
    return d.toLocaleDateString();
}
