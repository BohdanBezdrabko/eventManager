package com.example.sportadministrationsystem.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Конфігурація для вирішення проблем із ConcurrentModificationException.
 * Відключає OpenEntityManagerInViewInterceptor, який може викликати помилки
 * при закритті EntityManager в асинхронних контекстах.
 */
@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // НЕ додаємо OpenEntityManagerInViewInterceptor
        // Замість цього, всі запити до БД повинні бути явно завантажені в трансакціях
        log.debug("WebConfig: OEMVI не активовано - явне завантаження або fetch JOIN");
    }
}
