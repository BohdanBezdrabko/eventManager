
package com.example.sportadministrationsystem.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity @Table(name="post_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PostTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @Column(nullable=false, unique=true, length=64) private String code;
    @Column(nullable=false, length=128) private String name;

    @Column(name="title_tpl", nullable=false, columnDefinition="TEXT") private String titleTpl;
    @Column(name="body_tpl",  nullable=false, columnDefinition="TEXT") private String bodyTpl;

    @Column(name="offset_days",  nullable=false) private int offsetDays;
    @Column(name="offset_hours", nullable=false) private int offsetHours;

    @Column(nullable=false) private boolean active = true;

    @Enumerated(EnumType.STRING) @Column(length=64) private EventCategory category; // nullable
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=32) private Audience audience;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=32) private Channel channel;

    @CreationTimestamp @Column(name="created_at", updatable=false) private LocalDateTime createdAt;
    @UpdateTimestamp   @Column(name="updated_at") private LocalDateTime updatedAt;
}
