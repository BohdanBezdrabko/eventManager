// src/main/java/com/example/sportadministrationsystem/repository/PostTemplateRepository.java
package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.Audience;
import com.example.sportadministrationsystem.model.Channel;
import com.example.sportadministrationsystem.model.EventCategory;
import com.example.sportadministrationsystem.model.PostTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostTemplateRepository extends JpaRepository<PostTemplate, Long> {
    @Query("""
        select t from PostTemplate t
        where t.active = true and (t.category is null or t.category = :cat)
        """)
    List<PostTemplate> findActiveForCategory(@Param("cat") EventCategory cat);

    /**
     * Знайти всі активні шаблони для каналу та аудиторії
     */
    List<PostTemplate> findByChannelAndAudienceAndActive(Channel channel, Audience audience, boolean active);

    /**
     * Знайти активний шаблон за кодом
     */
    Optional<PostTemplate> findByCodeAndActive(String code, boolean active);
}


