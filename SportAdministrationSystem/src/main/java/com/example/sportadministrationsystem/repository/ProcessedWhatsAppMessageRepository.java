package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.ProcessedWhatsAppMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessedWhatsAppMessageRepository extends JpaRepository<ProcessedWhatsAppMessage, Long> {

    /**
     * Перевірити, чи дане повідомлення вже було обработлено
     */
    Optional<ProcessedWhatsAppMessage> findByMessageId(String messageId);

    /**
     * Перевірити наявність повідомлення (для ідемпотентності)
     */
    boolean existsByMessageId(String messageId);
}
