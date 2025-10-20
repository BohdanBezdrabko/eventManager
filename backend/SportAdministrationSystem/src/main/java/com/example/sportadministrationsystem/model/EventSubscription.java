package com.example.sportadministrationsystem.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "event_subscriptions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_event_user_messenger",
                columnNames = {"event_id", "user_id", "messenger"} // V10 + унікальний ключ
        )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class EventSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)  // V10: event_id FK
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)  // V10: user_id FK
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // V13: messenger VARCHAR(16) + CHECK('TELEGRAM')
    @Enumerated(EnumType.STRING)
    @Column(name = "messenger", nullable = false, length = 16)
    private Messenger messenger;

    // V10: active BOOLEAN NOT NULL DEFAULT TRUE ; V15: колонку created_at видалено
    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
