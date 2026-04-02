package com.example.sportadministrationsystem.model;

/**
 * Типи сповіщень для WhatsApp
 * Кожен тип відповідає окремому Meta-approved шаблону
 */
public enum NotificationType {
    /**
     * Підтвердження підписки на івент
     * Шаблон: event_subscription_confirm
     */
    SUBSCRIPTION_CONFIRM,

    /**
     * Підтвердження реєстрації на івент
     * Шаблон: event_registration_confirm
     */
    REGISTRATION_CONFIRM,

    /**
     * Нагадування за 72 години до подій
     * Шаблон: event_reminder_72h
     */
    REMINDER_72H,

    /**
     * Нагадування за 24 години до подій
     * Шаблон: event_reminder_24h
     */
    REMINDER_24H,

    /**
     * Сповіщення про оновлення івенту
     * Шаблон: event_updated
     */
    EVENT_UPDATED,

    /**
     * Сповіщення про скасування івенту
     * Шаблон: event_cancelled
     */
    EVENT_CANCELLED
}
