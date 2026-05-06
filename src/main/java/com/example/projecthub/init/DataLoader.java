package com.example.projecthub.init;

import com.example.projecthub.entity.Comment;
import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.ProjectStatus;
import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskStatus;
import com.example.projecthub.entity.TimeEntry;
import com.example.projecthub.entity.User;
import com.example.projecthub.repository.CommentRepository;
import com.example.projecthub.repository.ProjectRepository;
import com.example.projecthub.repository.TaskRepository;
import com.example.projecthub.repository.TimeEntryRepository;
import com.example.projecthub.repository.UserRepository;
import com.example.projecthub.service.UserService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Инициализация демо-данных при старте приложения.
 * Учётки: admin/admin123 (ADMIN), ivan/user123, maria/user123 (USER).
 */
@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final UserService userService;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    private final TimeEntryRepository timeEntryRepository;

    @Value("${projecthub.seed.enabled:true}")
    private boolean seedEnabled;

    public DataLoader(UserService userService,
                      UserRepository userRepository,
                      ProjectRepository projectRepository,
                      TaskRepository taskRepository,
                      CommentRepository commentRepository,
                      TimeEntryRepository timeEntryRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.commentRepository = commentRepository;
        this.timeEntryRepository = timeEntryRepository;
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

        User admin = userService.createUser("admin", "admin123", Role.ADMIN);
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

        // Демо-записи учёта времени, чтобы дашборд и профиль не были пустыми.
        LocalDateTime now = LocalDateTime.now();
        TimeEntry e1 = new TimeEntry(t1, ivan, now.minusDays(1).withHour(10).withMinute(0));
        e1.stop(now.minusDays(1).withHour(11).withMinute(45), "Подготовка DDL для users/projects");
        timeEntryRepository.save(e1);
        TimeEntry e2 = new TimeEntry(t1, ivan, now.minusHours(3));
        e2.stop(now.minusHours(2).minusMinutes(15), "Доделал tasks и comments");
        timeEntryRepository.save(e2);
        TimeEntry e3 = new TimeEntry(t2, maria, now.minusDays(2).withHour(14).withMinute(0));
        e3.stop(now.minusDays(2).withHour(14).withMinute(40), "Набросала план тестов");
        timeEntryRepository.save(e3);

        log.info("Сидинг завершён: admin={}, ivan={}, maria={}, projects={}, tasks={}",
                admin.getId(), ivan.getId(), maria.getId(),
                projectRepository.count(), taskRepository.count());
        // archived используется для демонстрации фильтрации проектов по статусу
        log.debug("Создан архивный проект id={}", archived.getId());
    }
}
