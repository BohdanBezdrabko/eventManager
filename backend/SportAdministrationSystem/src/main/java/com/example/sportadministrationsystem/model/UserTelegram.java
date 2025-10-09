package com.example.sportadministrationsystem.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_telegram")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserTelegram {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "tg_user_id", nullable = false, unique = true)
    private Long tgUserId;

    @Column(name = "tg_chat_id", nullable = false, unique = true)
    private Long tgChatId;

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;
}
