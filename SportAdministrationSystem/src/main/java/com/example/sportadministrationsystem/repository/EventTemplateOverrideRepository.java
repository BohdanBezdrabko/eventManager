// src/main/java/com/example/sportadministrationsystem/repository/EventTemplateOverrideRepository.java
package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.EventTemplateOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventTemplateOverrideRepository extends JpaRepository<EventTemplateOverride, Long> {
    Optional<EventTemplateOverride> findByEvent_IdAndTemplate_Id(Long eventId, Long templateId);
}
