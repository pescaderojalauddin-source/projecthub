package com.example.projecthub.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Включает поддержку асинхронного выполнения {@code @Async}-методов.
 * ТЗ §10 «Многопоточность»: фоновое обновление сводной статистики выполняется
 * в отдельном пуле потоков.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /** Пул потоков для {@code @Async}-задач. */
    @Bean(name = "applicationTaskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("projecthub-async-");
        executor.initialize();
        return executor;
    }
}
