package com.example.sportadministrationsystem.Repositories;

import com.example.sportadministrationsystem.Models.UserRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRegistrationRepository extends JpaRepository<UserRegistration, Long> {
    List<UserRegistration> findByUser_Id(Long userId);
}
