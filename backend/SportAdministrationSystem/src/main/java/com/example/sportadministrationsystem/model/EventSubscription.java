package com.example.sportadministrationsystem.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "event_subscriptions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_event_userTelegram_messenger",
                columnNames = {"event_id", "user_telegram_id", "messenger"}
        )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class EventSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // тепер підписник — це Telegram-користувач
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_telegram_id", nullable = false)
    private UserTelegram userTelegram;

    @Enumerated(EnumType.STRING)
    @Column(name = "messenger", nullable = false, length = 16)
    private Messenger messenger;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
