package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.AnnouncementTemplate;
import com.example.sportadministrationsystem.model.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnnouncementTemplateRepository extends JpaRepository<AnnouncementTemplate, Long> {

    /**
     * Знайти всі шаблони для івенту
     */
    List<AnnouncementTemplate> findByEvent_IdOrderByCreatedAtDesc(Long eventId);

    /**
     * Знайти активні шаблони для івенту
     */
    List<AnnouncementTemplate> findByEvent_IdAndEnabledTrueOrderByCreatedAtDesc(Long eventId);

    /**
     * Знайти шаблон за ID
     */
    Optional<AnnouncementTemplate> findById(Long id);

    /**
     * Знайти шаблони за каналом для івенту
     */
    List<AnnouncementTemplate> findByEvent_IdAndChannelOrderByCreatedAtDesc(Long eventId, Channel channel);
}
