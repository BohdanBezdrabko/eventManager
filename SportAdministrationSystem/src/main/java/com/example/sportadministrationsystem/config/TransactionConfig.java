package com.example.sportadministrationsystem.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Конфігурація для управління транзакціями та избежания проблем з ConcurrentModificationException.
 *
 * Ключові точки:
 * 1. OpenEntityManagerInViewInterceptor ВІДКЛЮЧЕНА (видалена з WebConfig)
 *    - Це запобігає проблемам при закритті EntityManager в асинхронних контекстах
 * 2. Увімкнена @EnableTransactionManagement
 *    - Гарантує коректне управління транзакціями
 * 3. Усі запити до БД явно обгорнені в @Transactional методи
 */
@Slf4j
@Configuration
@EnableTransactionManagement
public class TransactionConfig {

    public TransactionConfig() {
        log.info("TransactionConfig initialized: OEMVI disabled, explicit @Transactional required");
    }
}
