package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.NotificationType;
import com.example.sportadministrationsystem.model.WhatsAppTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WhatsAppTemplateRepository extends JpaRepository<WhatsAppTemplate, Long> {

    /**
     * Знайти шаблон за типом сповіщення
     */
    Optional<WhatsAppTemplate> findByNotificationType(NotificationType notificationType);

    /**
     * Знайти шаблон за імʼєм
     */
    Optional<WhatsAppTemplate> findByTemplateName(String templateName);

    /**
     * Знайти активний шаблон за типом сповіщення
     */
    Optional<WhatsAppTemplate> findByNotificationTypeAndEnabledTrue(NotificationType notificationType);
}
