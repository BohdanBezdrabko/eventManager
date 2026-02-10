// src/components/EventSubscriptionInfo.jsx
import "./EventSubscriptionInfo.css";

export default function EventSubscriptionInfo({ eventName, startDate, location, tgCount, waCount }) {
    const formatDate = (dateStr) => {
        if (!dateStr) return "—";
        try {
            const date = new Date(dateStr);
            return new Intl.DateTimeFormat("uk-UA", {
                year: "numeric",
                month: "2-digit",
                day: "2-digit",
                hour: "2-digit",
                minute: "2-digit",
            }).format(date);
        } catch {
            return "—";
        }
    };

    return (
        <div className="event-subscription-info">
            <div className="esi-header">
                <h2 className="esi-title">{eventName || "Івент без назви"}</h2>
            </div>

            <div className="esi-details">
                {startDate && (
                    <div className="esi-detail-row">
                        <span className="esi-icon">🕐</span>
                        <span className="esi-text">{formatDate(startDate)}</span>
                    </div>
                )}

                {location && (
                    <div className="esi-detail-row">
                        <span className="esi-icon">📍</span>
                        <span className="esi-text">{location}</span>
                    </div>
                )}
            </div>

            <div className="esi-subscription-stats">
                <div className="esi-stat">
                    <span className="esi-stat-icon">📱</span>
                    <div className="esi-stat-content">
                        <div className="esi-stat-label">Telegram</div>
                        <div className="esi-stat-count">{tgCount ?? "—"}</div>
                    </div>
                </div>

                <div className="esi-stat">
                    <span className="esi-stat-icon">💬</span>
                    <div className="esi-stat-content">
                        <div className="esi-stat-label">WhatsApp</div>
                        <div className="esi-stat-count">{waCount ?? "—"}</div>
                    </div>
                </div>
            </div>

            <div className="esi-description">
                <p className="esi-desc-text">
                    Люди, які підписались на цей івент, отримуватимуть оновлення через обрані ними канали.
                </p>
            </div>
        </div>
    );
}
