// src/pages/CreateEventPage.jsx
import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { createEvent } from "@/services/events.jsx";

export default function CreateEventPage() {
    const navigate = useNavigate();
    const [form, setForm] = useState({
        name: "",
        startAt: "", // очікуємо "YYYY-MM-DDTHH:mm" з <input type="datetime-local">
        location: "",
        capacity: "",
        coverUrl: "",
        description: "",
    });
    const [submitting, setSubmitting] = useState(false);
    const [err, setErr] = useState(null);

    function onChange(e) {
        const { name, value } = e.target;
        setForm((f) => ({ ...f, [name]: value }));
    }

    // Локальний datetime → ISO без часової зони (сумісно з Spring LocalDateTime)
    function normalizeLocalDateTime(s) {
        if (!s) return null;
        if (s.length === 16) return s + ":00";
        if (s.length >= 19) return s.slice(0, 19);
        return s;
    }

    async function onSubmit(e) {
        e.preventDefault();
        setErr(null);
        setSubmitting(true);
        try {
            const payload = {
                name: form.name.trim(),
                startAt: normalizeLocalDateTime(form.startAt),
                location: form.location.trim(),
                capacity: form.capacity ? Number(form.capacity) : null,
                coverUrl: form.coverUrl?.trim() || null,
                description: form.description?.trim() || null,
            };
            const saved = await createEvent(payload);
            navigate(`/events/${saved.id}`);
        } catch (e2) {
            setErr(e2?.message || "Помилка створення івенту");
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <div className="cep-wrap">
            <div className="cep-card">
                <h1 className="cep-title">Створити івент</h1>

                {err && (
                    <div role="alert" className="cep-alert">
                        {err}
                    </div>
                )}

                <form onSubmit={onSubmit} className="cep-form">
                    <div className="cep-field">
                        <label htmlFor="name" className="cep-label">Назва</label>
                        <input
                            id="name"
                            name="name"
                            type="text"
                            required
                            value={form.name}
                            onChange={onChange}
                            placeholder="Напр., Volleyball Open"
                            className="cep-input"
                            autoComplete="off"
                        />
                    </div>

                    <div className="cep-field">
                        <label htmlFor="startAt" className="cep-label">Початок</label>
                        <input
                            id="startAt"
                            name="startAt"
                            type="datetime-local"
                            required
                            value={form.startAt}
                            onChange={onChange}
                            className="cep-input"
                        />
                    </div>

                    <div className="cep-field">
                        <label htmlFor="location" className="cep-label">Локація</label>
                        <input
                            id="location"
                            name="location"
                            type="text"
                            required
                            value={form.location}
                            onChange={onChange}
                            placeholder="Місто, адреса або назва залу"
                            className="cep-input"
                            autoComplete="off"
                        />
                    </div>

                    <div className="cep-field">
                        <label htmlFor="capacity" className="cep-label">Місткість</label>
                        <input
                            id="capacity"
                            name="capacity"
                            type="number"
                            min="1"
                            inputMode="numeric"
                            value={form.capacity}
                            onChange={onChange}
                            placeholder="Напр., 50"
                            className="cep-input"
                        />
                    </div>

                    <div className="cep-field">
                        <label htmlFor="coverUrl" className="cep-label">Обкладинка (URL)</label>
                        <input
                            id="coverUrl"
                            name="coverUrl"
                            type="url"
                            value={form.coverUrl}
                            onChange={onChange}
                            placeholder="https://..."
                            className="cep-input"
                            autoComplete="off"
                        />
                    </div>

                    <div className="cep-field">
                        <label htmlFor="description" className="cep-label">Опис</label>
                        <textarea
                            id="description"
                            name="description"
                            rows={5}
                            value={form.description}
                            onChange={onChange}
                            placeholder="Короткий опис івенту"
                            className="cep-textarea"
                        />
                    </div>

                    <div className="cep-actions">
                        <button type="submit" disabled={submitting} className="btn btn-primary">
                            {submitting ? "Створюємо" : "Створити"}
                        </button>
                        <Link to="/events" className="btn btn-ghost">Скасувати</Link>
                    </div>
                </form>
            </div>

            {/* CSS у цьому ж файлі */}
            <style>{`
        .cep-wrap {
          --bg: #0b0d10;
          --card: #12161b;
          --muted: #8a94a7;
          --text: #e6eaf0;
          --border: #1f2630;
          --accent: #3b82f6;
          --accent-2: #2563eb;
          display: grid;
          place-items: start center;
          min-height: 100%;
          padding: 24px 16px;
          background: var(--bg);
          color: var(--text);
        }
        .cep-card {
          width: 100%;
          max-width: 760px;
          background: var(--card);
          border: 1px solid var(--border);
          border-radius: 16px;
          padding: 20px;
          box-shadow: 0 8px 24px rgba(0,0,0,0.35);
        }
        .cep-title {
          margin: 0 0 12px 0;
          font-size: 22px;
          font-weight: 700;
          letter-spacing: 0.2px;
        }
        .cep-alert {
          background: rgba(220, 38, 38, 0.12);
          border: 1px solid rgba(220, 38, 38, 0.35);
          color: #fecaca;
          padding: 10px 12px;
          border-radius: 10px;
          margin-bottom: 12px;
          font-size: 14px;
        }
        .cep-form { display: grid; gap: 12px; }
        .cep-field { display: grid; gap: 6px; }
        .cep-label {
          font-size: 13px;
          color: var(--muted);
        }
        .cep-input, .cep-textarea {
          width: 100%;
          background: #0f1318;
          color: var(--text);
          border: 1px solid var(--border);
          border-radius: 10px;
          padding: 10px 12px;
          outline: none;
          font-size: 14px;
          transition: border-color 0.15s ease, box-shadow 0.15s ease;
        }
        .cep-input:focus, .cep-textarea:focus {
          border-color: var(--accent);
          box-shadow: 0 0 0 3px rgba(59,130,246,0.15);
        }
        .cep-input::placeholder, .cep-textarea::placeholder {
          color: #667085;
        }
        .cep-textarea { resize: vertical; min-height: 110px; }
        .cep-actions {
          display: flex;
          gap: 10px;
          margin-top: 6px;
        }
        .btn {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          gap: 8px;
          padding: 10px 16px;
          border-radius: 10px;
          border: 1px solid var(--border);
          cursor: pointer;
          text-decoration: none;
          font-weight: 600;
          font-size: 14px;
          transition: transform 0.06s ease, background 0.15s ease, border-color 0.15s ease;
          user-select: none;
        }
        .btn:active { transform: translateY(1px); }
        .btn-primary {
          background: var(--accent);
          border-color: var(--accent-2);
          color: white;
        }
        .btn-primary:disabled {
          opacity: 0.7;
          cursor: default;
        }
        .btn-primary:hover:not(:disabled) {
          background: var(--accent-2);
        }
        .btn-ghost {
          background: transparent;
          color: var(--text);
        }
        .btn-ghost:hover {
          background: #0f1318;
        }

        @media (max-width: 560px) {
          .cep-card { padding: 16px; border-radius: 12px; }
          .cep-actions { flex-direction: column; align-items: stretch; }
        }
      `}</style>
        </div>
    );
}
