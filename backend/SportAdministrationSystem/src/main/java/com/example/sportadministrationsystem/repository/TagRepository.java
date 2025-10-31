package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    // Якщо в тебе вже є інший метод — залиш свій і підкоригуй виклик у сервісі
    Optional<Tag> findByNameIgnoreCase(String name);
}
