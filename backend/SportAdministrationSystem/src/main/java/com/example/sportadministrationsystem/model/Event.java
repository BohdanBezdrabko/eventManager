package com.example.sportadministrationsystem.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import com.example.sportadministrationsystem.model.User;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)               // V2: name TEXT NOT NULL
    private String name;

    @Column(nullable = false)               // V2: location TEXT NOT NULL
    private String location;

    @CreationTimestamp                      // V2: created_at TIMESTAMP NOT NULL DEFAULT NOW()
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @Column(name = "registered_count", nullable = false) // V2: registered_count INTEGER NOT NULL DEFAULT 0
    private Integer registeredCount = 0;

    @Column(name = "start_at", nullable = false) // V5: start_at TIMESTAMP NOT NULL DEFAULT NOW()
    private LocalDateTime startAt;

    @Column                                  // V5: capacity INTEGER
    private Integer capacity;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    @Column(columnDefinition = "TEXT")       // V5: description TEXT
    private String description;

    @Column(name = "cover_url")              // V5: cover_url TEXT
    private String coverUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 64)
    private EventCategory category;

    @ManyToMany
    @JoinTable(
            name = "event_tags",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();
}
