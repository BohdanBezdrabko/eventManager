package com.example.sportadministrationsystem.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "event_subscriptions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_event_user_messenger",
                columnNames = {"event_id", "user_id", "messenger"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "messenger", nullable = false, length = 16)
    private Messenger messenger;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
