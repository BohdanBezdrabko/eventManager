package com.example.sportadministrationsystem.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "event_subscriptions_whatsapp",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_event_user_whatsapp",
                columnNames = {"event_id", "user_whatsapp_id"}
        )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class EventSubscriptionWhatsapp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_whatsapp_id", nullable = false)
    private UserWhatsapp userWhatsapp;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
