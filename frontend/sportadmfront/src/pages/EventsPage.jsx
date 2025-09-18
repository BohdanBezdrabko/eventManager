// src/pages/EventsPage.jsx
import { useEffect, useMemo, useState } from "react";
import {
    getAllEvents,
    getEventsByLocation,
    getEventsByName,
} from "../services/events.js";
import { Search, MapPin, Calendar } from "lucide-react";

export default function EventsPage() {
    const [events, setEvents] = useState([]);
    const [mode, setMode] = useState("name"); // 'name' | 'location'
    const [query, setQuery] = useState("");
    const [loading, setLoading] = useState(true);
    const [searching, setSearching] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        (async () => {
            try {
                setLoading(true);
                const data = await getAllEvents();
                setEvents(Array.isArray(data) ? data : [data]);
            } catch (e) {
                setError(e.message || "Помилка завантаження");
            } finally {
                setLoading(false);
            }
        })();
    }, []);

    const handleSearch = async (e) => {
        e?.preventDefault();
        setError(null);

        if (!query.trim()) {
            try {
                setSearching(true);
                const data = await getAllEvents();
                setEvents(Array.isArray(data) ? data : [data]);
            } catch (e) {
                setError(e.message || "Помилка пошуку");
            } finally {
                setSearching(false);
            }
            return;
        }

        try {
            setSearching(true);
            const data =
                mode === "name"
                    ? await getEventsByName(query.trim())
                    : await getEventsByLocation(query.trim());
            setEvents(Array.isArray(data) ? data : [data]);
        } catch (e) {
            setError(e.message || "Помилка пошуку");
            setEvents([]);
        } finally {
            setSearching(false);
        }
    };

    const title = useMemo(
        () => (mode === "name" ? "Пошук за назвою" : "Пошук за локацією"),
        [mode]
    );

    if (loading) {
        return (
            <div className="container-fluid vh-100 d-flex justify-content-center align-items-center">
                <div className="text-center">
                    <div className="spinner-border text-primary" role="status"></div>
                    <p className="mt-2 text-muted">Завантаження подій...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="container-fluid py-4">
            {/* Панель пошуку зверху */}
            <div className="mb-4">
                <form
                    onSubmit={handleSearch}
                    className="d-flex flex-column flex-md-row align-items-md-center gap-3"
                >
                    {/* Поле пошуку */}
                    <div className="input-group flex-grow-1">
                        <span className="input-group-text bg-body-tertiary border-0 rounded-start-pill">
                            <Search size={18} className="text-muted" />
                        </span>
                        <input
                            type="text"
                            className="form-control border-0 bg-body-tertiary shadow-sm rounded-end-pill"
                            placeholder={
                                mode === "name"
                                    ? "Шукати подію за назвою..."
                                    : "Шукати подію за локацією..."
                            }
                            value={query}
                            onChange={(e) => setQuery(e.target.value)}
                        />
                    </div>

                    {/* Режим пошуку */}
                    <div className="btn-group" role="group">
                        <input
                            type="radio"
                            className="btn-check"
                            name="searchMode"
                            id="mode-name"
                            autoComplete="off"
                            checked={mode === "name"}
                            onChange={() => setMode("name")}
                        />
                        <label
                            className="btn btn-outline-primary rounded-pill px-3"
                            htmlFor="mode-name"
                        >
                            Назва
                        </label>

                        <input
                            type="radio"
                            className="btn-check"
                            name="searchMode"
                            id="mode-location"
                            autoComplete="off"
                            checked={mode === "location"}
                            onChange={() => setMode("location")}
                        />
                        <label
                            className="btn btn-outline-primary rounded-pill px-3"
                            htmlFor="mode-location"
                        >
                            Локація
                        </label>
                    </div>

                    {/* Кнопка пошуку */}
                    <button
                        type="submit"
                        className="btn btn-primary rounded-pill px-4"
                        disabled={searching}
                    >
                        {searching ? "Пошук…" : "Пошук"}
                    </button>
                </form>
            </div>

            {/* Контент з подіями */}
            <div className="row">
                <div className="col-12">
                    {error && (
                        <div className="alert text-bg-danger mb-3" role="alert">
                            {error}
                        </div>
                    )}

                    {events.length === 0 ? (
                        <div className="alert text-bg-secondary text-center py-4">
                            Нічого не знайдено
                        </div>
                    ) : (
                        <div className="row g-4">
                            {events.map((ev) => (
                                <div className="col-12 col-md-6 col-lg-4" key={ev.id}>
                                    <div className="card shadow-sm h-100 border-0 rounded-4">
                                        <div className="card-body d-flex flex-column">
                                            <h5 className="card-title text-body-emphasis fw-bold mb-2">
                                                {ev.name}
                                            </h5>
                                            <p className="text-muted mb-3 d-flex flex-column">
                                                <span className="d-flex align-items-center">
                                                    <MapPin size={16} className="me-1 text-muted" />
                                                    {ev.location}
                                                </span>
                                                <span className="d-flex align-items-center">
                                                    <Calendar size={16} className="me-1 text-muted" />
                                                    {formatDate(ev.date)}
                                                </span>
                                            </p>
                                            <div className="mt-auto">
                                                <button className="btn btn-outline-primary w-100 rounded-pill d-flex justify-content-center align-items-center gap-2">
                                                    Детальніше
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}

function formatDate(isoLike) {
    if (!isoLike) return "-";
    try {
        const d = new Date(isoLike);
        if (Number.isNaN(d.getTime())) return isoLike;
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, "0");
        const day = String(d.getDate()).padStart(2, "0");
        return `${y}-${m}-${day}`;
    } catch {
        return isoLike;
    }
}
