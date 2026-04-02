package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.DeliveryStatus;
import com.example.sportadministrationsystem.model.NotificationType;
import com.example.sportadministrationsystem.model.WhatsAppDeliveryTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WhatsAppDeliveryTrackingRepository extends JpaRepository<WhatsAppDeliveryTracking, Long> {

    /**
     * Знайти повідомлення за messageId
     */
    List<WhatsAppDeliveryTracking> findByMessageId(String messageId);

    /**
     * Знайти повідомлення за одержувачем та статусом
     */
    List<WhatsAppDeliveryTracking> findByRecipientAndStatus(String recipient, DeliveryStatus status);

    /**
     * Знайти невдані повідомлення для івенту
     */
    List<WhatsAppDeliveryTracking> findByEventIdAndStatus(Long eventId, DeliveryStatus status);

    /**
     * Знайти повідомлення за типом сповіщення і статусом
     */
    List<WhatsAppDeliveryTracking> findByNotificationTypeAndStatus(
            NotificationType notificationType,
            DeliveryStatus status
    );

    /**
     * Знайти недавні повідомлення (для моніторингу доставки)
     */
    @Query("SELECT d FROM WhatsAppDeliveryTracking d WHERE d.sentAt > :since ORDER BY d.sentAt DESC")
    List<WhatsAppDeliveryTracking> findRecentMessages(@Param("since") LocalDateTime since);

    /**
     * Статистика доставки за період
     */
    @Query("SELECT COUNT(*) FROM WhatsAppDeliveryTracking d WHERE d.sentAt >= :start AND d.sentAt <= :end AND d.status = :status")
    long countByPeriodAndStatus(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") DeliveryStatus status
    );
}
