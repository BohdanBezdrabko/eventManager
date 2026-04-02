package com.example.sportadministrationsystem.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Таблиця для трекінгу доставки вихідних WhatsApp повідомлень
 */
@Entity
@Table(name = "whatsapp_delivery_tracking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppDeliveryTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Унікальний ID повідомлення від WhatsApp API (повертається при sendText/sendTemplate)
     */
    @Column(name = "message_id", length = 255)
    private String messageId;

    /**
     * WhatsApp ID одержувача
     */
    @Column(name = "recipient", nullable = false, length = 50)
    private String recipient;

    /**
     * Імʼя шаблону (для template messages) або null для plain text
     */
    @Column(name = "template_name", length = 255)
    private String templateName;

    /**
     * Тип сповіщення (REMINDER_24H, EVENT_UPDATED тощо)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 50)
    private NotificationType notificationType;

    /**
     * ID івенту (для контексту)
     */
    @Column(name = "event_id")
    private Long eventId;

    /**
     * Статус доставки (SENT, DELIVERED, FAILED, READ)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.SENT;

    /**
     * Коли повідомлення було відправлено
     */
    @CreationTimestamp
    @Column(name = "sent_at", updatable = false)
    private LocalDateTime sentAt;

    /**
     * Коли повідомлення було доставлено
     */
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    /**
     * Коли повідомлення було прочитано
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * Коли був останній раз оновлений статус
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Опис помилки (якщо status=FAILED)
     */
    @Column(name = "error_description", columnDefinition = "TEXT")
    private String errorDescription;
}
