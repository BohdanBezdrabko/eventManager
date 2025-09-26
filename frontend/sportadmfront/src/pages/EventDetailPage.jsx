import { useEffect, useState, useCallback } from "react";
import { useParams, Link } from "react-router-dom";
import { getEventById } from "@/services/events.jsx";
import {
    registerForEvent,
    cancelRegistration,
    getMyRegistrations,
} from "@/services/eventRegistrations.jsx";

export default function EventDetailsPage() {
    const { id } = useParams();
    const [ev, setEv] = useState(null);
    const [registered, setRegistered] = useState(false);
    const [loading, setLoading] = useState(true);
    const [err, setErr] = useState("");

    useEffect(() => {
        let off = false;
        (async () => {
            try {
                setLoading(true);
                const [eventData, myRegs] = await Promise.all([
                    getEventById(id),
                    getMyRegistrations(),
                ]);
                if (off) return;
                setEv(eventData);
                const regIds = new Set(
                    (Array.isArray(myRegs) ? myRegs : [])
                        .map((r) => r?.event?.id ?? r?.eventId)
                        .filter(Boolean)
                );
                setRegistered(regIds.has(Number(id)));
            } catch (e) {
                if (!off) setErr(e?.message || "Не вдалося завантажити івент");
            } finally {
                if (!off) setLoading(false);
            }
        })();
        return () => {
            off = true;
        };
    }, [id]);

    const onRegister = useCallback(async () => {
        try {
            await registerForEvent(id);
            setRegistered(true);
            // за потреби — перезавантажити подію, щоб оновити лічильник
            try {
                const fresh = await getEventById(id);
                setEv(fresh);
            } catch {}
        } catch (e) {
            setErr(e?.message || "Помилка реєстрації");
        }
    }, [id]);

    const onCancel = useCallback(async () => {
        try {
            await cancelRegistration(id);
            setRegistered(false);
            try {
                const fresh = await getEventById(id);
                setEv(fresh);
            } catch {}
        } catch (e) {
            setErr(e?.message || "Помилка скасування");
        }
    }, [id]);

    if (loading) return <div className="container py-4">Завантаження…</div>;
    if (err) return <div className="container py-4 text-danger">{err}</div>;
    if (!ev) return <div className="container py-4">Подію не знайдено</div>;

    return (
        <div className="container py-4">
            <Link to="/events" className="btn btn-outline-secondary mb-3">
                ← До списку
            </Link>
            <h1 className="mb-2">{ev.name || ev.title || "Подія"}</h1>
            <div className="text-muted mb-3">
                {(ev.startAt || ev.date || ev.datetime) ?? "без дати"} • {ev.location || "—"}
            </div>
            <p className="mb-4">{ev.description || "Опис відсутній."}</p>
            <div className="d-flex gap-2">
                {!registered ? (
                    <button className="btn btn-primary" onClick={onRegister}>
                        Зареєструватися
                    </button>
                ) : (
                    <button className="btn btn-outline-danger" onClick={onCancel}>
                        Скасувати
                    </button>
                )}
            </div>
        </div>
    );
}
