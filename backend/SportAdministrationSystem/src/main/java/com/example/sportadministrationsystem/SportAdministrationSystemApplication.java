package com.example.sportadministrationsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SportAdministrationSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(SportAdministrationSystemApplication.class, args);
    }
}
