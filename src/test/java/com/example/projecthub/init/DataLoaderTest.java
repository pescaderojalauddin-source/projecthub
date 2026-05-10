package com.example.projecthub.init;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.projecthub.entity.Role;
import com.example.projecthub.repository.CommentRepository;
import com.example.projecthub.repository.ProjectRepository;
import com.example.projecthub.repository.TaskRepository;
import com.example.projecthub.repository.UserRepository;
import com.example.projecthub.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * Тесты на безопасное поведение {@link DataLoader} в проде:
 * <ul>
 *     <li>Без явного пароля админа в не-{@code dev} профиле админ <b>не создаётся</b>.</li>
 *     <li>С заданным {@code projecthub.seed.admin-password} — создаётся под этим паролем.</li>
 *     <li>{@code projecthub.seed.demo-data-enabled=false} отключает сидинг ivan/maria и проектов.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "projecthub.seed.enabled=false")
@Transactional
class DataLoaderTest {

    @Autowired
    DataLoader dataLoader;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    TaskRepository taskRepository;
    @Autowired
    CommentRepository commentRepository;
    @Autowired
    UserService userService;

    @BeforeEach
    void cleanup() {
        commentRepository.deleteAll();
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
        // Профиль 'test' — не 'dev', поэтому fallback не применяется.
        ReflectionTestUtils.setField(dataLoader, "seedEnabled", true);
        ReflectionTestUtils.setField(dataLoader, "adminPassword", "");
        ReflectionTestUtils.setField(dataLoader, "demoDataEnabled", false);
    }

    @Test
    void doesNotSeedAdminWithoutPasswordInNonDevProfile() {
        dataLoader.run();

        assertThat(userRepository.findByLogin("admin")).isEmpty();
    }

    @Test
    void seedsAdminWhenPasswordIsProvided() {
        ReflectionTestUtils.setField(dataLoader, "adminPassword", "S3cret-pass!");

        dataLoader.run();

        assertThat(userRepository.findByLogin("admin")).isPresent()
                .get()
                .satisfies(u -> assertThat(u.getRole()).isEqualTo(Role.ADMIN));
    }

    @Test
    void demoDataNotSeededWhenDisabled() {
        ReflectionTestUtils.setField(dataLoader, "adminPassword", "S3cret-pass!");
        ReflectionTestUtils.setField(dataLoader, "demoDataEnabled", false);

        dataLoader.run();

        assertThat(userRepository.findByLogin("ivan")).isEmpty();
        assertThat(userRepository.findByLogin("maria")).isEmpty();
        assertThat(projectRepository.count()).isZero();
    }

    @Test
    void demoDataSeededWhenEnabled() {
        ReflectionTestUtils.setField(dataLoader, "adminPassword", "S3cret-pass!");
        ReflectionTestUtils.setField(dataLoader, "demoDataEnabled", true);

        dataLoader.run();

        assertThat(userRepository.findByLogin("ivan")).isPresent();
        assertThat(userRepository.findByLogin("maria")).isPresent();
        assertThat(projectRepository.count()).isGreaterThan(0L);
        assertThat(taskRepository.count()).isGreaterThan(0L);
    }

    @Test
    void seedDisabledShortCircuits() {
        ReflectionTestUtils.setField(dataLoader, "seedEnabled", false);
        ReflectionTestUtils.setField(dataLoader, "adminPassword", "S3cret-pass!");

        dataLoader.run();

        assertThat(userRepository.count()).isZero();
    }
}
