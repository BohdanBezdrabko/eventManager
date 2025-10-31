package com.example.sportadministrationsystem.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_telegram")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserTelegram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Більше НІЯКОГО посилання на таблицю users (адміни)

    @Column(name = "tg_user_id", nullable = false, unique = true)
    private Long tgUserId;

    // Для приватних чатів chat_id == user_id; зберігаємо окремо, бо для груп інше
    @Column(name = "tg_chat_id", nullable = false, unique = true)
    private Long tgChatId;

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;

    @PrePersist
    void prePersist() {
        if (linkedAt == null) {
            linkedAt = LocalDateTime.now();
        }
    }
}
