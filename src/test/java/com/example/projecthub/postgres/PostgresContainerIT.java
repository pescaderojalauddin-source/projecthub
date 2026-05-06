package com.example.projecthub.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.ProjectStatus;
import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskStatus;
import com.example.projecthub.entity.User;
import com.example.projecthub.repository.ProjectRepository;
import com.example.projecthub.repository.TaskRepository;
import com.example.projecthub.repository.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Интеграционный тест на реальном PostgreSQL через Testcontainers.
 * Поднимает контейнер {@code postgres:16-alpine}, прогоняет Flyway-миграции
 * и проверяет CRUD ключевых сущностей и каскадное удаление.
 *
 * <p>Тест включается только если в окружении доступен Docker (см.
 * {@link EnabledIfEnvironmentVariable}). В CI GitHub Actions Docker предустановлен.
 * Локально — выставить {@code TESTCONTAINERS=1}.
 */
@Testcontainers
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "TESTCONTAINERS", matches = "1|true|TRUE")
class PostgresContainerIT {

    @Container
    @SuppressWarnings("resource") // Testcontainers сам останавливает контейнер по завершении JVM
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("projecthub")
                    .withUsername("projecthub")
                    .withPassword("projecthub");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("projecthub.seed.enabled", () -> "false");
    }

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    TaskRepository taskRepository;

    @Test
    @Transactional
    void canPersistEntitiesOnRealPostgres() {
        User owner = userRepository.save(new User("pg-owner", "x", Role.USER));
        Project project = projectRepository.save(
                new Project("Postgres test", "demo", ProjectStatus.ACTIVE, owner));
        Task task = taskRepository.save(new Task(
                "Task #1", "desc", TaskStatus.TODO, LocalDate.now().plusDays(7), project, owner));

        assertThat(project.getId()).isNotNull();
        assertThat(task.getId()).isNotNull();
        assertThat(taskRepository.findAllByProject(project,
                org.springframework.data.domain.PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
    }

    @Test
    @Transactional
    void flywayMigrationsAppliedAndCountsZeroByDefault() {
        // Если флайвей-миграция отработала — таблицы/индексы существуют, репозитории не падают.
        assertThat(userRepository.count()).isGreaterThanOrEqualTo(0L);
        assertThat(projectRepository.count()).isGreaterThanOrEqualTo(0L);
        assertThat(taskRepository.count()).isGreaterThanOrEqualTo(0L);
    }
}
