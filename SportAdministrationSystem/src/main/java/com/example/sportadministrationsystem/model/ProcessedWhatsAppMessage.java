package com.example.sportadministrationsystem.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Таблиця для трекінгу обробних повідомлень WhatsApp
 * Забезпечує ідемпотентність webhook обробки
 */
@Entity
@Table(
        name = "processed_whatsapp_messages",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_message_id",
                columnNames = {"message_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedWhatsAppMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Унікальний ID повідомлення від WhatsApp API
     */
    @Column(name = "message_id", nullable = false, length = 255)
    private String messageId;

    /**
     * WhatsApp ID користувача, який надіслав повідомлення (from)
     */
    @Column(name = "wa_id", nullable = false, length = 50)
    private String waId;

    /**
     * Коли повідомлення було отримано (timestamp з webhook)
     */
    @Column(name = "webhook_timestamp")
    private LocalDateTime webhookTimestamp;

    /**
     * Коли був оброблено
     */
    @CreationTimestamp
    @Column(name = "processed_at", updatable = false)
    private LocalDateTime processedAt;

    /**
     * Тип повідомлення (text, interactive, button)
     */
    @Column(name = "message_type", length = 50)
    private String messageType;

    /**
     * Короткий опис повідомлення для логування
     */
    @Column(name = "message_summary", columnDefinition = "TEXT")
    private String messageSummary;
}
