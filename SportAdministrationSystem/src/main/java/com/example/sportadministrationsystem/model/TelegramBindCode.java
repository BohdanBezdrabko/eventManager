package com.example.sportadministrationsystem.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "telegram_bind_codes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TelegramBindCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 32)
    private String code;

    @Column(name = "tg_group_chat_id", nullable = false)
    private Long tgGroupChatId;

    @Column(name = "group_name", length = 255)
    private String groupName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used", nullable = false)
    private Boolean used = false;

    @Column(name = "used_by_user_id")
    private Long usedByUserId;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (expiresAt == null) expiresAt = LocalDateTime.now().plusHours(24);
        if (used == null) used = false;
    }
}
