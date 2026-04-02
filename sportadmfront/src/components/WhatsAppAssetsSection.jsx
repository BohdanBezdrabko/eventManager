// src/components/WhatsAppAssetsSection.jsx
import { useEffect, useState } from "react";
import { previewTemplate } from "@/services/announcementTemplates.jsx";
import "./WhatsAppAssetsSection.css";

/**
 * Компонент для відображення WhatsApp активів івенту:
 * - wa.me посилання
 * - Preview анонсу
 * - Кількість підписників
 * - Кнопки копіювання
 */
export default function WhatsAppAssetsSection({ event, eventId }) {
    const [templates, setTemplates] = useState([]);
    const [selectedTemplateId, setSelectedTemplateId] = useState(null);
    const [preview, setPreview] = useState(null);
    const [loading, setLoading] = useState(false);

    const waLink = buildWaLink(eventId);

    useEffect(() => {
        if (selectedTemplateId) {
            loadPreview(eventId, selectedTemplateId);
        }
    }, [selectedTemplateId, eventId]);

    const loadPreview = async (eId, tId) => {
        setLoading(true);
        try {
            const data = await previewTemplate(eId, tId);
            setPreview(data);
        } catch (err) {
            console.error("Failed to load preview:", err);
        } finally {
            setLoading(false);
        }
    };

    const copyToClipboard = async (text, label) => {
        try {
            await navigator.clipboard.writeText(text);
            alert(`✅ ${label} скопійовано!`);
        } catch (err) {
            alert("❌ Не вдалось скопіювати");
        }
    };

    return (
        <div className="wa-assets-section">
            <h3>📱 WhatsApp посилання</h3>

            {/* wa.me посилання */}
            <div className="wa-assets-item">
                <h4>wa.me посилання для підписки</h4>
                <div className="wa-assets-link">
                    <code>{waLink}</code>
                    <button
                        className="btn-copy"
                        onClick={() => copyToClipboard(waLink, "wa.me посилання")}
                    >
                        📋 Копіювати
                    </button>
                </div>
                <p className="hint">
                    Поділіться цим посиланням, щоб користувачі могли підписатися через WhatsApp
                </p>
            </div>

            {/* Preview шаблону */}
            {templates.length > 0 && (
                <div className="wa-assets-item">
                    <h4>Попередження анонсу</h4>
                    <select
                        className="template-select"
                        value={selectedTemplateId || ""}
                        onChange={(e) => setSelectedTemplateId(e.target.value ? Number(e.target.value) : null)}
                    >
                        <option value="">Виберіть шаблон...</option>
                        {templates.map((t) => (
                            <option key={t.id} value={t.id}>
                                {t.templateTitle}
                            </option>
                        ))}
                    </select>

                    {loading && <p className="muted">⏳ Завантаження...</p>}

                    {preview && (
                        <div className="preview-box">
                            <h5>Попередження:</h5>
                            <div className="preview-text">
                                {preview.renderedText}
                            </div>
                            <button
                                className="btn-copy"
                                onClick={() => copyToClipboard(preview.renderedText, "Анонс")}
                            >
                                📋 Копіювати весь текст
                            </button>
                        </div>
                    )}
                </div>
            )}

            {/* Кількість підписників */}
            {event && (
                <div className="wa-assets-item">
                    <h4>👥 Підписники WhatsApp</h4>
                    <p className="stats">
                        <strong>{event.waSubscriberCount || 0}</strong> активних підписників
                    </p>
                </div>
            )}
        </div>
    );
}

function buildWaLink(eventId) {
    // Це буде заповнено з конфіга з бекенду
    const businessPhone = "+15551944111"; // Замініть реальним номером
    return `https://wa.me/${businessPhone}?text=START%20${eventId}`;
}
