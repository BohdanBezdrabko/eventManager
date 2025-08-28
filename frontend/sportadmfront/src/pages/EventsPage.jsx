// src/pages/EventsPage.jsx
import { useEffect, useMemo, useState } from 'react';
import { getAllEvents, getEventsByLocation, getEventsByName } from '../services/events';

export default function EventsPage() {
    const [events, setEvents] = useState([]);
    const [mode, setMode] = useState('name'); // 'name' | 'location'
    const [query, setQuery] = useState('');
    const [loading, setLoading] = useState(true);
    const [searching, setSearching] = useState(false);
    const [error, setError] = useState(null);

    // початкове завантаження всіх подій
    useEffect(() => {
        (async () => {
            try {
                setLoading(true);
                const data = await getAllEvents();
                setEvents(Array.isArray(data) ? data : [data]); // на випадок якщо бек повертає один об’єкт
            } catch (e) {
                setError(e.message || 'Помилка завантаження');
            } finally {
                setLoading(false);
            }
        })();
    }, []);

    const handleSearch = async (e) => {
        e?.preventDefault();
        setError(null);

        // якщо рядок порожній — показуємо всі
        if (!query.trim()) {
            try {
                setSearching(true);
                const data = await getAllEvents();
                setEvents(Array.isArray(data) ? data : [data]);
            } catch (e) {
                setError(e.message || 'Помилка пошуку');
            } finally {
                setSearching(false);
            }
            return;
        }

        try {
            setSearching(true);
            const data =
                mode === 'name'
                    ? await getEventsByName(query.trim())
                    : await getEventsByLocation(query.trim());
            // якщо ендпоінт повертає один об’єкт — уніфікуємо до масиву
            setEvents(Array.isArray(data) ? data : [data]);
        } catch (e) {
            setError(e.message || 'Помилка пошуку');
            setEvents([]);
        } finally {
            setSearching(false);
        }
    };

    const title = useMemo(
        () => (mode === 'name' ? 'Пошук за назвою' : 'Пошук за локацією'),
        [mode]
    );

    if (loading) {
        return (
            <div className="container-fluid vh-100 d-flex justify-content-center align-items-center">
                <div className="spinner-border" role="status">
                    <span className="visually-hidden">Завантаження...</span>
                </div>
            </div>
        );
    }

    return (
        <div className="container py-4">
            <div className="card shadow-lg">
                <div className="card-body">
                    <div className="d-flex align-items-center justify-content-between flex-wrap gap-3 mb-3">
                        <h3 className="card-title mb-0">Події</h3>

                        <div className="btn-group" role="group" aria-label="Режим пошуку">
                            <input
                                type="radio"
                                className="btn-check"
                                name="searchMode"
                                id="mode-name"
                                autoComplete="off"
                                checked={mode === 'name'}
                                onChange={() => setMode('name')}
                            />
                            <label className="btn btn-outline-secondary" htmlFor="mode-name">
                                За назвою
                            </label>

                            <input
                                type="radio"
                                className="btn-check"
                                name="searchMode"
                                id="mode-location"
                                autoComplete="off"
                                checked={mode === 'location'}
                                onChange={() => setMode('location')}
                            />
                            <label className="btn btn-outline-secondary" htmlFor="mode-location">
                                За локацією
                            </label>
                        </div>
                    </div>

                    <form className="row g-2 mb-3" onSubmit={handleSearch}>
                        <div className="col-12 col-md">
                            <input
                                type="text"
                                className="form-control"
                                placeholder={mode === 'name' ? 'Введіть назву події' : 'Введіть локацію'}
                                value={query}
                                onChange={(e) => setQuery(e.target.value)}
                            />
                        </div>
                        <div className="col-12 col-md-auto d-flex gap-2">
                            <button type="submit" className="btn btn-dark" disabled={searching}>
                                {searching ? 'Пошук…' : 'Пошук'}
                            </button>
                            <button
                                type="button"
                                className="btn btn-outline-secondary"
                                onClick={() => {
                                    setQuery('');
                                    handleSearch();
                                }}
                                disabled={searching}
                            >
                                Показати всі
                            </button>
                        </div>
                    </form>

                    {error && (
                        <div className="alert alert-danger" role="alert">
                            {error}
                        </div>
                    )}

                    <div className="table-responsive">
                        <table className="table table-hover align-middle">
                            <thead className="table-light">
                            <tr>
                                <th style={{ width: 80 }}>ID</th>
                                <th>Назва</th>
                                <th>Локація</th>
                                <th style={{ width: 180 }}>Дата</th>
                            </tr>
                            </thead>
                            <tbody>
                            {events.length === 0 ? (
                                <tr>
                                    <td colSpan="4" className="text-center text-muted py-4">
                                        Нічого не знайдено
                                    </td>
                                </tr>
                            ) : (
                                events.map((e) => (
                                    <tr key={e.id}>
                                        <td>{e.id}</td>
                                        <td className="fw-semibold">{e.name}</td>
                                        <td>{e.location}</td>
                                        <td>{formatDate(e.date)}</td>
                                    </tr>
                                ))
                            )}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    );
}

function formatDate(isoLike) {
    if (!isoLike) return '-';
    try {
        const d = new Date(isoLike);
        if (Number.isNaN(d.getTime())) return isoLike;
        // YYYY-MM-DD
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${y}-${m}-${day}`;
    } catch {
        return isoLike;
    }
}
