package com.example.sportadministrationsystem.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_telegram") // V10
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserTelegram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // V10: user_id UNIQUE NOT NULL
    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    // V10: tg_user_id BIGINT NOT NULL UNIQUE
    @Column(name = "tg_user_id", nullable = false, unique = true)
    private Long tgUserId;

    // V10: tg_chat_id BIGINT NOT NULL UNIQUE (для private чату chat_id == user_id, можна ставити tgChatId = tgUserId)
    @Column(name = "tg_chat_id", nullable = false, unique = true)
    private Long tgChatId;

    // V10: linked_at TIMESTAMP NOT NULL DEFAULT NOW()
    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;

    @PrePersist
    void prePersist() {
        if (linkedAt == null) {
            linkedAt = LocalDateTime.now();
        }
    }
}
