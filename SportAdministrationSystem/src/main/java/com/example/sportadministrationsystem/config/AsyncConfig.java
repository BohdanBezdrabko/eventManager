package com.example.sportadministrationsystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Конфігурація для асинхронної обробки (webhook-и, dispatch-и).
 * Дозволяє методам помічені @Async виконуватися в окремих потоках.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${async.core-pool-size:5}")
    private int corePoolSize;

    @Value("${async.max-pool-size:20}")
    private int maxPoolSize;

    @Value("${async.queue-capacity:100}")
    private int queueCapacity;

    @Value("${async.thread-name-prefix:async-}")
    private String threadNamePrefix;

    /**
     * ThreadPoolTaskExecutor для @Async методів.
     * За замовчуванням Spring використовує SimpleAsyncTaskExecutor (новий потік для кожної задачі).
     * Цей bean їх переконфігурує.
     */
    @Bean(name = "taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
