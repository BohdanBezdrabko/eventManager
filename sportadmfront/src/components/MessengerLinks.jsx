// src/components/MessengerLinks.jsx
import { useState } from "react";
import { getTelegramLinkUrl, getWhatsAppLinkUrl } from "@/services/events.jsx";

export default function MessengerLinks() {
    const [tgUrl, setTgUrl] = useState(null);
    const [waUrl, setWaUrl] = useState(null);
    const [tgLoading, setTgLoading] = useState(false);
    const [waLoading, setWaLoading] = useState(false);
    const [error, setError] = useState("");

    const fetchTelegramLink = async () => {
        try {
            setTgLoading(true);
            setError("");
            const url = await getTelegramLinkUrl();
            setTgUrl(url);
        } catch (err) {
            setError(err?.message || "Не вдалося отримати посилання на Telegram");
        } finally {
            setTgLoading(false);
        }
    };

    const fetchWhatsAppLink = async () => {
        try {
            setWaLoading(true);
            setError("");
            const url = await getWhatsAppLinkUrl();
            setWaUrl(url);
        } catch (err) {
            setError(err?.message || "Не вдалося отримати посилання на WhatsApp");
        } finally {
            setWaLoading(false);
        }
    };

    return (
        <div className="messenger-links">
            <style>{styles}</style>

            <h3 className="ml-title">Зв'язок з месенджерами</h3>

            {error && <div className="ml-error">{error}</div>}

            <div className="ml-grid">
                <div className="ml-card">
                    <div className="ml-icon">📱</div>
                    <h4 className="ml-card-title">Telegram</h4>
                    <p className="ml-card-desc">Отримуйте оновлення про івенти у Telegram</p>

                    {tgUrl ? (
                        <a href={tgUrl} target="_blank" rel="noopener noreferrer" className="ml-btn ml-btn--linked">
                            ✓ Перейти до бота
                        </a>
                    ) : (
                        <button
                            className="ml-btn ml-btn--primary"
                            onClick={fetchTelegramLink}
                            disabled={tgLoading || waLoading}
                        >
                            {tgLoading ? "Завантаження…" : "Зв'язати з Telegram"}
                        </button>
                    )}
                </div>

                <div className="ml-card">
                    <div className="ml-icon">💬</div>
                    <h4 className="ml-card-title">WhatsApp</h4>
                    <p className="ml-card-desc">Отримуйте оновлення про івенти у WhatsApp</p>

                    {waUrl ? (
                        <a href={waUrl} target="_blank" rel="noopener noreferrer" className="ml-btn ml-btn--linked">
                            ✓ Відкрити WhatsApp
                        </a>
                    ) : (
                        <button
                            className="ml-btn ml-btn--primary"
                            onClick={fetchWhatsAppLink}
                            disabled={tgLoading || waLoading}
                        >
                            {waLoading ? "Завантаження…" : "Зв'язати з WhatsApp"}
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
}

const styles = `
.messenger-links {
    padding: 0;
}

.ml-title {
    font-size: 16px;
    font-weight: 600;
    margin: 0 0 12px 0;
    color: #e8eef6;
}

.ml-error {
    padding: 12px;
    border-radius: 10px;
    background: #3b0f14;
    border: 1px solid #a83a46;
    color: #ffd5d8;
    margin-bottom: 12px;
    font-size: 13px;
}

.ml-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
}

.ml-card {
    background: #0f1826;
    border: 1px solid #ffffff1a;
    border-radius: 12px;
    padding: 14px;
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    transition: border-color 0.2s ease, background-color 0.2s ease;
}

.ml-card:hover {
    border-color: #5b8cff55;
    background: #121c2b;
}

.ml-icon {
    font-size: 32px;
    margin-bottom: 8px;
}

.ml-card-title {
    margin: 0 0 6px 0;
    font-size: 14px;
    font-weight: 600;
    color: #e8eef6;
}

.ml-card-desc {
    margin: 0 0 10px 0;
    font-size: 12px;
    color: #9fb2c7;
}

.ml-btn {
    padding: 8px 14px;
    border-radius: 10px;
    border: 1px solid #ffffff22;
    background: #0b1220;
    color: #e8eef6;
    cursor: pointer;
    font-size: 12px;
    font-weight: 500;
    text-decoration: none;
    display: inline-block;
    transition: all 0.2s ease;
    text-align: center;
    width: 100%;
}

.ml-btn--primary {
    background: linear-gradient(145deg, #5b8cff, #4c7fff);
    border-color: #5b8cff55;
    color: white;
}

.ml-btn--primary:hover:not(:disabled) {
    filter: brightness(1.1);
    transform: translateY(-1px);
}

.ml-btn--primary:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

.ml-btn--linked {
    background: linear-gradient(145deg, #3ad0a1, #2abf91);
    border-color: #3ad0a155;
    color: white;
}

.ml-btn--linked:hover {
    filter: brightness(1.1);
    transform: translateY(-1px);
}

@media (max-width: 768px) {
    .ml-grid {
        grid-template-columns: 1fr;
    }
}
`;
