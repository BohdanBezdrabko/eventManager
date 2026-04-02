package com.example.sportadministrationsystem.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Meta-approved WhatsApp шаблон
 * Зберігає маппінг між NotificationType та імʼєм шаблону на Meta Business Account
 */
@Entity
@Table(
        name = "whatsapp_templates",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_notification_type",
                columnNames = {"notification_type"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Тип сповіщення (напр. REMINDER_24H)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    /**
     * Імʼя шаблону на Meta Business Account (напр. "event_reminder_24h")
     */
    @Column(name = "template_name", nullable = false, length = 255)
    private String templateName;

    /**
     * Код мови для шаблону (напр. "uk" для українського)
     */
    @Column(name = "language_code", nullable = false, length = 10)
    @Builder.Default
    private String languageCode = "uk";

    /**
     * Описування шаблону для адмінів
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Активний/неактивний статус
     */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /**
     * Коли був створений цей запис
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Коли був останній раз оновлений
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
