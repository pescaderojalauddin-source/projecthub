package com.example.projecthub.init;

import com.example.projecthub.entity.Comment;
import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.ProjectStatus;
import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskStatus;
import com.example.projecthub.entity.User;
import com.example.projecthub.repository.CommentRepository;
import com.example.projecthub.repository.ProjectRepository;
import com.example.projecthub.repository.TaskRepository;
import com.example.projecthub.repository.UserRepository;
import com.example.projecthub.service.UserService;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Инициализация демо-данных при старте приложения.
 *
 * <p>Поведение управляется свойствами {@code projecthub.seed.*}:
 * <ul>
 *     <li>{@code projecthub.seed.enabled} (env {@code PROJECTHUB_SEED_ENABLED}) — мастер-выключатель.</li>
 *     <li>{@code projecthub.seed.admin-login} (env {@code PROJECTHUB_SEED_ADMIN_LOGIN}, по умолчанию {@code admin}).</li>
 *     <li>{@code projecthub.seed.admin-password} (env {@code PROJECTHUB_SEED_ADMIN_PASSWORD}) — пароль админа.
 *         Если не задан в проде — админ <b>не создаётся</b>, в логи пишется предупреждение.
 *         В профиле {@code dev} есть удобный fallback на {@code admin123} для локальной разработки.</li>
 *     <li>{@code projecthub.seed.demo-data-enabled} (env {@code PROJECTHUB_SEED_DEMO_DATA_ENABLED}) — сидить ли
 *         тестовых пользователей {@code ivan/maria} и пример проектов. По умолчанию — {@code true}.</li>
 * </ul>
 *
 * <p>Это предотвращает регрессию, когда деплой в проде стартовал с захардкоженным паролем {@code admin/admin123}
 * и любой желающий мог войти как админ.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    /** Fallback-пароль для профиля {@code dev}, чтобы не ломать локальную разработку и существующие тесты. */
    private static final String DEV_FALLBACK_ADMIN_PASSWORD = "admin123";

    private final UserService userService;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    private final Environment environment;

    @Value("${projecthub.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${projecthub.seed.admin-login:admin}")
    private String adminLogin;

    @Value("${projecthub.seed.admin-password:}")
    private String adminPassword;

    @Value("${projecthub.seed.demo-data-enabled:true}")
    private boolean demoDataEnabled;

    public DataLoader(UserService userService,
                      UserRepository userRepository,
                      ProjectRepository projectRepository,
                      TaskRepository taskRepository,
                      CommentRepository commentRepository,
                      Environment environment) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.commentRepository = commentRepository;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }
        if (userRepository.count() > 0) {
            log.info("Сидинг пропущен: пользователи уже есть в БД.");
            return;
        }
        log.info("Сидинг демо-данных…");

        User admin = seedAdmin();

        if (!demoDataEnabled) {
            log.info("Сидинг demo-data отключён (projecthub.seed.demo-data-enabled=false).");
            return;
        }

        seedDemoData(admin);
    }

    /**
     * Создаёт админа, если задан пароль через свойство/env.
     * В профиле {@code dev} — fallback на {@code admin123} для локальной разработки.
     * В остальных профилях без явного пароля админ не создаётся.
     */
    private User seedAdmin() {
        String resolvedPassword = adminPassword;
        if (resolvedPassword == null || resolvedPassword.isBlank()) {
            if (isDevProfile()) {
                log.warn("projecthub.seed.admin-password не задан — использую dev-fallback '{}'. "
                        + "В проде задайте PROJECTHUB_SEED_ADMIN_PASSWORD.", DEV_FALLBACK_ADMIN_PASSWORD);
                resolvedPassword = DEV_FALLBACK_ADMIN_PASSWORD;
            } else {
                log.warn("Сидинг админа пропущен: PROJECTHUB_SEED_ADMIN_PASSWORD не задан. "
                        + "Чтобы создать первого ADMIN, задайте переменную окружения и перезапустите сервис.");
                return null;
            }
        }
        User admin = userService.createUser(adminLogin, resolvedPassword, Role.ADMIN);
        log.info("Сидинг ADMIN id={} login={}", admin.getId(), admin.getLogin());
        return admin;
    }

    private void seedDemoData(User admin) {
        User ivan = userService.createUser("ivan", "user123", Role.USER);
        User maria = userService.createUser("maria", "user123", Role.USER);

        Project migration = projectRepository.save(new Project(
                "Миграция БД",
                "Перевод сервиса с MySQL на PostgreSQL.",
                ProjectStatus.ACTIVE,
                ivan));

        Project landing = projectRepository.save(new Project(
                "Лендинг ProjectHub",
                "Маркетинговая страница и форма обратной связи.",
                ProjectStatus.ACTIVE,
                maria));

        Project archived = projectRepository.save(new Project(
                "Архивный проект 2024",
                "Старый проект, переведён в архив.",
                ProjectStatus.ARCHIVED,
                ivan));

        Task t1 = taskRepository.save(new Task(
                "Написать SQL-скрипты миграции",
                "Создать DDL для PostgreSQL.",
                TaskStatus.IN_PROGRESS,
                LocalDate.now().plusDays(7),
                migration,
                ivan));
        Task t2 = taskRepository.save(new Task(
                "Покрыть тесты",
                "Добавить интеграционные тесты на репозиторий.",
                TaskStatus.TODO,
                LocalDate.now().plusDays(14),
                migration,
                maria));
        taskRepository.save(new Task(
                "Сверстать главную",
                "HTML/CSS макет главной страницы.",
                TaskStatus.DONE,
                LocalDate.now().minusDays(2),
                landing,
                maria));
        taskRepository.save(new Task(
                "Подготовить контент",
                "Тексты, иллюстрации.",
                TaskStatus.BLOCKED,
                LocalDate.now().plusDays(3),
                landing,
                ivan));

        commentRepository.save(new Comment("Готово, прошу проверить.", t1, ivan));
        commentRepository.save(new Comment("Сверила схему — есть пара замечаний, оставлю отдельно.", t1, maria));
        commentRepository.save(new Comment("Беру задачу.", t2, maria));

        log.info("Сидинг demo-data завершён: admin={}, ivan={}, maria={}, projects={}, tasks={}",
                admin != null ? admin.getId() : null, ivan.getId(), maria.getId(),
                projectRepository.count(), taskRepository.count());
        log.debug("Создан архивный проект id={}", archived.getId());
    }

    private boolean isDevProfile() {
        List<String> active = Arrays.asList(environment.getActiveProfiles());
        return active.contains("dev") || active.isEmpty(); // дефолтный профиль в Spring Boot — пустой → dev
    }
}
