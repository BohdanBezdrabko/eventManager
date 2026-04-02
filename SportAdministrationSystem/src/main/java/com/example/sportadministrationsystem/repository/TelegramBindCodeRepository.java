package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.TelegramBindCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TelegramBindCodeRepository extends JpaRepository<TelegramBindCode, Long> {

    /**
     * Знайти невикористаний код, якщо ще не закінчився дедлайн
     */
    @Query("""
        select tbc from TelegramBindCode tbc
        where tbc.code = :code
          and tbc.used = false
          and tbc.expiresAt > :now
        """)
    Optional<TelegramBindCode> findValidCode(@Param("code") String code, @Param("now") LocalDateTime now);

    /**
     * Знайти або створити для групи (простий пошук)
     */
    Optional<TelegramBindCode> findByTgGroupChatId(Long tgGroupChatId);
}
