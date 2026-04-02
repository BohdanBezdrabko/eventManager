// src/pages/TemplateManagerPage.jsx
import { useState, useEffect } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import {
    listTemplates,
    createTemplate,
    updateTemplate,
    deleteTemplate,
    previewTemplate,
} from "@/services/announcementTemplates.jsx";
import "./TemplateManagerPage.css";

export default function TemplateManagerPage() {
    const { id: eventId } = useParams();
    const navigate = useNavigate();

    const [templates, setTemplates] = useState([]);
    const [loading, setLoading] = useState(true);
    const [err, setErr] = useState("");
    const [showForm, setShowForm] = useState(false);
    const [editingId, setEditingId] = useState(null);

    const [formData, setFormData] = useState({
        templateTitle: "",
        templateBody: "",
        channel: "WHATSAPP",
        notes: "",
    });

    const [preview, setPreview] = useState(null);
    const [previewLoading, setPreviewLoading] = useState(false);

    useEffect(() => {
        loadTemplates();
    }, [eventId]);

    const loadTemplates = async () => {
        setLoading(true);
        setErr("");
        try {
            const data = await listTemplates(eventId);
            setTemplates(Array.isArray(data) ? data : []);
        } catch (e) {
            setErr(e?.message || "Не вдалось завантажити шаблони");
            setTemplates([]);
        } finally {
            setLoading(false);
        }
    };

    const handleAddNew = () => {
        setFormData({
            templateTitle: "",
            templateBody: "",
            channel: "WHATSAPP",
            notes: "",
        });
        setEditingId(null);
        setShowForm(true);
        setPreview(null);
    };

    const handleEdit = (template) => {
        setFormData({
            templateTitle: template.templateTitle,
            templateBody: template.templateBody,
            channel: template.channel,
            notes: template.notes || "",
        });
        setEditingId(template.id);
        setShowForm(true);
        setPreview(null);
    };

    const handleLoadPreview = async () => {
        if (!editingId) return;

        setPreviewLoading(true);
        try {
            const data = await previewTemplate(eventId, editingId);
            setPreview(data);
        } catch (e) {
            alert("❌ Не вдалось завантажити попередження: " + e?.message);
        } finally {
            setPreviewLoading(false);
        }
    };

    const handleSave = async (e) => {
        e.preventDefault();

        if (!formData.templateTitle.trim()) {
            alert("❌ Назва шаблону обов'язкова");
            return;
        }
        if (!formData.templateBody.trim()) {
            alert("❌ Текст шаблону обов'язковий");
            return;
        }

        try {
            if (editingId) {
                await updateTemplate(eventId, editingId, formData);
                alert("✅ Шаблон оновлено!");
            } else {
                await createTemplate(eventId, formData);
                alert("✅ Шаблон створено!");
            }
            setShowForm(false);
            await loadTemplates();
        } catch (e) {
            alert("❌ Помилка: " + e?.message);
        }
    };

    const handleDelete = async (templateId) => {
        if (!confirm("Ви впевнені?")) return;

        try {
            await deleteTemplate(eventId, templateId);
            alert("✅ Шаблон видалено!");
            await loadTemplates();
        } catch (e) {
            alert("❌ Помилка: " + e?.message);
        }
    };

    const handleCancel = () => {
        setShowForm(false);
        setEditingId(null);
        setPreview(null);
    };

    return (
        <div className="container template-manager-page">
            <div className="tm-header">
                <h1>📋 Менеджер шаблонів оголошень</h1>
                <Link to={`/events/${eventId}`} className="btn btn-ghost">
                    ← До івенту
                </Link>
            </div>

            {err && <div className="alert alert-danger">{err}</div>}

            {loading ? (
                <div className="muted">⏳ Завантаження...</div>
            ) : (
                <>
                    {/* Форма */}
                    {showForm ? (
                        <div className="tm-form-container">
                            <div className="tm-form-box">
                                <h2>{editingId ? "Редагувати шаблон" : "Новий шаблон"}</h2>

                                <form onSubmit={handleSave} className="tm-form">
                                    <div className="form-group">
                                        <label>Назва шаблону</label>
                                        <input
                                            type="text"
                                            className="form-input"
                                            value={formData.templateTitle}
                                            onChange={(e) =>
                                                setFormData({ ...formData, templateTitle: e.target.value })
                                            }
                                            placeholder="Напр. Оголошення у групу"
                                        />
                                    </div>

                                    <div className="form-group">
                                        <label>Текст шаблону</label>
                                        <textarea
                                            className="form-input"
                                            rows={10}
                                            value={formData.templateBody}
                                            onChange={(e) =>
                                                setFormData({ ...formData, templateBody: e.target.value })
                                            }
                                            placeholder={`Використовуйте плейсхолдери:
{{event_name}} - назва івенту
{{event_date}} - дата (dd.MM.yyyy)
{{event_time}} - час (HH:mm)
{{event_datetime}} - дата і час
{{location}} - місцезнаходження
{{description}} - опис
{{wa_link}} - wa.me посилання для підписки
{{event_page_url}} - URL сторінки івенту`}
                                        />
                                    </div>

                                    <div className="form-group">
                                        <label>Канал</label>
                                        <select
                                            className="form-input"
                                            value={formData.channel}
                                            onChange={(e) =>
                                                setFormData({ ...formData, channel: e.target.value })
                                            }
                                        >
                                            <option value="WHATSAPP">WhatsApp</option>
                                            <option value="TELEGRAM">Telegram</option>
                                        </select>
                                    </div>

                                    <div className="form-group">
                                        <label>Примітки (опційно)</label>
                                        <textarea
                                            className="form-input"
                                            rows={3}
                                            value={formData.notes}
                                            onChange={(e) =>
                                                setFormData({ ...formData, notes: e.target.value })
                                            }
                                            placeholder="Внутрішні примітки для вас"
                                        />
                                    </div>

                                    <div className="form-actions">
                                        <button type="submit" className="btn btn-primary">
                                            💾 Зберегти
                                        </button>
                                        {editingId && (
                                            <button
                                                type="button"
                                                className="btn btn-secondary"
                                                onClick={handleLoadPreview}
                                                disabled={previewLoading}
                                            >
                                                {previewLoading ? "⏳ Завантаження..." : "👁️ Переглянути"}
                                            </button>
                                        )}
                                        <button
                                            type="button"
                                            className="btn btn-ghost"
                                            onClick={handleCancel}
                                        >
                                            ✕ Скасувати
                                        </button>
                                    </div>
                                </form>
                            </div>

                            {/* Preview */}
                            {preview && (
                                <div className="tm-preview-box">
                                    <h3>Попередження:</h3>
                                    <div className="preview-content">
                                        {preview.renderedText}
                                    </div>
                                </div>
                            )}
                        </div>
                    ) : (
                        <>
                            {/* Кнопка додати новий */}
                            <button className="btn btn-primary" onClick={handleAddNew}>
                                ➕ Новий шаблон
                            </button>

                            {/* Таблиця шаблонів */}
                            {templates.length === 0 ? (
                                <div className="empty-state">
                                    <p>📭 Немає шаблонів</p>
                                    <p className="hint">Створіть перший шаблон, щоб почати</p>
                                </div>
                            ) : (
                                <div className="tm-table-container">
                                    <table className="tm-table">
                                        <thead>
                                            <tr>
                                                <th>Назва</th>
                                                <th>Канал</th>
                                                <th>Статус</th>
                                                <th>Дія</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {templates.map((template) => (
                                                <tr key={template.id}>
                                                    <td>
                                                        <strong>{template.templateTitle}</strong>
                                                    </td>
                                                    <td>{template.channel}</td>
                                                    <td>
                                                        {template.enabled ? (
                                                            <span className="badge badge-active">✓ Активний</span>
                                                        ) : (
                                                            <span className="badge badge-inactive">✕ Неактивний</span>
                                                        )}
                                                    </td>
                                                    <td>
                                                        <button
                                                            className="btn btn-sm btn-primary"
                                                            onClick={() => handleEdit(template)}
                                                        >
                                                            ✎ Редагувати
                                                        </button>
                                                        <button
                                                            className="btn btn-sm btn-danger"
                                                            onClick={() => handleDelete(template.id)}
                                                        >
                                                            🗑️ Видалити
                                                        </button>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </>
                    )}
                </>
            )}
        </div>
    );
}
