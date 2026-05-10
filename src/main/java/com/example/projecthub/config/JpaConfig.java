package com.example.projecthub.config;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Включает JPA Auditing: автоматическую простановку {@code @CreatedDate}, {@code @LastModifiedDate},
 * {@code @CreatedBy}, {@code @LastModifiedBy} на сущностях.
 *
 * <p>{@link #auditorProvider()} читает текущего аутентифицированного пользователя из
 * {@link SecurityContextHolder}. Для системных операций (CommandLineRunner, scheduled tasks,
 * тесты без security-контекста) подставляет {@code "system"}.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    /** Идентификатор «системного» актора для аудит-полей, когда нет аутентификации. */
    public static final String SYSTEM_AUDITOR = "system";

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return Optional.of(SYSTEM_AUDITOR);
            }
            return Optional.of(auth.getName());
        };
    }
}
