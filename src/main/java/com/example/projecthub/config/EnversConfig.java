package com.example.projecthub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Подключает Hibernate Envers через Spring Data Envers.
 *
 * <p>Заменяет дефолтный {@code JpaRepositoryFactoryBean} на
 * {@link EnversRevisionRepositoryFactoryBean} — это даёт репозиториям, которые расширяют
 * {@code RevisionRepository} (см. {@link com.example.projecthub.repository.TaskRepository}),
 * методы {@code findRevisions(...)}, {@code findLastChangeRevision(...)} и др.
 *
 * <p>Обычные {@code JpaRepository}-репозитории продолжают работать как и раньше — фабрика
 * Envers совместима с базовым SimpleJpaRepository и просто оборачивает его.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "com.example.projecthub.repository",
        repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class)
public class EnversConfig {
}
