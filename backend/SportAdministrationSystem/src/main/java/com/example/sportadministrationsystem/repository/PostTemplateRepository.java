// src/main/java/com/example/sportadministrationsystem/repository/PostTemplateRepository.java
package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.PostTemplate;
import com.example.sportadministrationsystem.model.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostTemplateRepository extends JpaRepository<PostTemplate, Long> {
    @Query("""
        select t from PostTemplate t
        where t.active = true and (t.category is null or t.category = :cat)
        """)
    List<PostTemplate> findActiveForCategory(@Param("cat") EventCategory cat);
}
