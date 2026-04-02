package com.example.sportadministrationsystem.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Шаблон оголошення для ручної публікації у WhatsApp групи
 * Адміністратори копіюють сгенеровані тексти у WhatsApp групи вручну
 */
@Entity
@Table(name = "announcement_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Посилання на івент
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /**
     * Назва шаблону (для ідентифікації)
     */
    @Column(name = "template_title", nullable = false, length = 255)
    private String templateTitle;

    /**
     * Тіло шаблону з плейсхолдерами:
     * {{event_name}}, {{event_date}}, {{event_time}}, {{location}}, {{description}},
     * {{wa_link}}, {{event_page_url}}
     */
    @Column(name = "template_body", nullable = false, columnDefinition = "TEXT")
    private String templateBody;

    /**
     * Канал распространения (WHATSAPP, TELEGRAM тощо)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 50)
    @Builder.Default
    private Channel channel = Channel.WHATSAPP;

    /**
     * Активний/неактивний статус
     */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /**
     * Примітки для адміністратора
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Коли був створений цей шаблон
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Коли був останній раз оновлений
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
