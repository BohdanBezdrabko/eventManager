
package com.example.sportadministrationsystem.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.Column;

import java.time.LocalDateTime;

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

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private LocalDateTime startAt;      // внутрішнє поле часу

    @Column(nullable = false, length = 255)
    private String location;

    private Integer capacity;

    @Column(length = 2048)
    private String description;

    @Column(length = 2048)
    private String coverUrl;

    @CreationTimestamp
    private LocalDateTime createdAt;
    @Column(name = "registered_count", nullable = false)
    private Integer registeredCount = 0;

    public Integer getRegisteredCount() {
        return registeredCount;
    }

    public void setRegisteredCount(Integer registeredCount) {
        this.registeredCount = registeredCount;
    }
}
