// src/main/java/com/example/sportadministrationsystem/model/EventSubscription.java
package com.example.sportadministrationsystem.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// ⬇️ ДОДАЙТЕ ці імпорти
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "event_subscriptions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id","user_id","messenger"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventSubscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false) @JoinColumn(name="event_id")
    private Event event;

    @ManyToOne(optional=false) @JoinColumn(name="user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Messenger messenger;

    @Column(nullable=false)
    private boolean active = true;

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;
}
