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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
 *         богатый набор демо-команд / проектов / задач. По умолчанию — {@code true}.</li>
 * </ul>
 *
 * <p>Демо-набор: 8 команд (Backend / Frontend / Mobile / DevOps / QA / Data / ML / Design), ~25 пользователей,
 * по 7+ проектов на пользователя (личные) и кросс-командные проекты под админом, задачи во всех статусах.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    /** Fallback-пароль для профиля {@code dev}, чтобы не ломать локальную разработку и существующие тесты. */
    private static final String DEV_FALLBACK_ADMIN_PASSWORD = "admin123";
    /** Дефолтный пароль для всех демо-юзеров. */
    private static final String DEMO_USER_PASSWORD = "user123";

    private final UserService userService;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    private final Environment environment;
    private final PlatformTransactionManager transactionManager;

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
                      Environment environment,
                      PlatformTransactionManager transactionManager) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.commentRepository = commentRepository;
        this.environment = environment;
        this.transactionManager = transactionManager;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }
        if (userRepository.count() > 0) {
            log.info("Сидинг пропущен: пользователи уже есть в БД.");
            return;
        }
        log.info("Сидинг демо-данных…");

        TransactionTemplate tt = new TransactionTemplate(transactionManager);

        User admin = tt.execute(status -> seedAdmin());

        if (!demoDataEnabled) {
            log.info("Сидинг demo-data отключён (projecthub.seed.demo-data-enabled=false).");
            return;
        }

        tt.execute(status -> {
            seedDemoData(admin);
            return null;
        });

        // Несколько отдельных транзакций — чтобы Envers зафиксировал несколько ревизий по одной задаче.
        seedTaskHistoryRevisions(tt);

        log.info("Сидинг demo-data завершён: users={}, projects={}, tasks={}, comments={}",
                userRepository.count(), projectRepository.count(),
                taskRepository.count(), commentRepository.count());
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

    /** Команда разработки: лид + члены + домен (для названий проектов и задач). */
    private record Team(String name,
                        String leadLogin,
                        List<String> memberLogins,
                        List<String> projectTemplates,
                        List<String> taskTemplates) {
    }

    private static List<Team> teams() {
        List<Team> teams = new ArrayList<>();

        teams.add(new Team(
                "Backend Core",
                "ivan",
                List.of("dmitry", "sergey", "anna"),
                List.of("API Gateway", "User Service", "Notifications Worker", "Order Service",
                        "Биллинг", "Audit Log", "Поиск (Elasticsearch)", "Реактивный профиль API",
                        "Импорт партнёрских прайсов", "Платёжный шлюз"),
                List.of("Спроектировать REST-эндпойнты", "Покрыть слой репозиториев тестами",
                        "Заменить N+1 на @EntityGraph", "Внедрить кэширование Redis",
                        "Подключить ShedLock на @Scheduled", "Перевести с JdbcTemplate на JPA",
                        "Добавить OpenAPI-схему", "Метрики Micrometer + Prometheus",
                        "Идемпотентность для POST", "Подключить Resilience4j", "Bulkhead для внешних HTTP",
                        "Логирование через MDC", "Контракт-тесты Pact")));

        teams.add(new Team(
                "Frontend Web",
                "maria",
                List.of("kirill", "maxim", "julia"),
                List.of("Веб-кабинет", "Дизайн-система", "Маркетинговый лендинг", "Внутренний портал",
                        "Партнёрский личный кабинет", "Админ-панель", "PWA-обёртка",
                        "Документация на VitePress", "Storybook компонентов", "A/B-тест чекаута"),
                List.of("Сверстать главную", "Прикрутить i18n (ru/en)", "Перевод на Vite 5",
                        "Оптимизировать LCP < 2.5s", "Lazy-load компоненты", "Темизация через CSS-переменные",
                        "Перевести FF на Bootstrap 5.3", "Тёмная тема + контраст AA", "E2E-тесты Playwright",
                        "Внедрить Storybook", "Аналитика событий через GTM",
                        "Skeleton-loader для медленных запросов", "ARIA-атрибуты для accessibility")));

        teams.add(new Team(
                "Mobile",
                "nikita",
                List.of("roman", "daria"),
                List.of("iOS App", "Android App", "Push-уведомления", "SDK для партнёров",
                        "Биометрия и Face ID", "Офлайн-режим", "Поддержка планшетов",
                        "Виджеты на главный экран", "Watch-приложение", "Авто-обновления через CodePush"),
                List.of("Перейти на SwiftUI", "Подключить Jetpack Compose", "Firebase Crashlytics",
                        "App Tracking Transparency", "Универсальные диплинки", "DeepLink-роутер",
                        "Поддержка Dark Mode", "Кеш изображений Glide/Kingfisher",
                        "Подключить in-app purchases", "Auto-renewal subscriptions",
                        "Сбор отзывов через in-app prompt", "Локализация под испанский")));

        teams.add(new Team(
                "DevOps & SRE",
                "vlad",
                List.of("igor", "lena"),
                List.of("Миграция в Kubernetes", "CI/CD рефакторинг", "Observability стек",
                        "Backup & DR", "Vault для секретов", "Service Mesh (Istio)",
                        "Continuous Delivery в Argo CD", "Multi-cluster setup", "FinOps дашборды",
                        "Внутренний PaaS"),
                List.of("Написать Helm-чарты", "ArgoCD для GitOps", "Мониторинг Grafana",
                        "Алерты Prometheus AlertManager", "Логи в Loki", "Tracing OpenTelemetry",
                        "Network policies", "Pod Security Standards", "HPA автоскейл по queue length",
                        "Backup PostgreSQL pg_basebackup", "Восстановление < 30 мин (RPO)",
                        "Sealed Secrets вместо ENV")));

        teams.add(new Team(
                "QA",
                "alex",
                List.of("vera", "pavel"),
                List.of("Регресс-тесты API", "Нагрузочное тестирование", "Smoke на staging",
                        "Автоматизация E2E", "Тестовый фреймворк (общий)", "Performance бюджеты",
                        "Контракт-тесты Pact (consumer)", "Тестовая среда для партнёров"),
                List.of("Покрыть критичный путь чекаута", "Поднять k6-сценарии",
                        "Параметризовать тесты", "Селектор по data-testid", "Visual regression",
                        "Тесты на a11y axe-core", "Мутационное тестирование PIT",
                        "Coverage gate > 70%", "Скриншоты при падении в CI",
                        "Параллельный запуск selenium grid", "Отчёт Allure")));

        teams.add(new Team(
                "Data & Analytics",
                "ekaterina",
                List.of("timofey", "marina"),
                List.of("DWH ETL", "Дашборд продаж", "Когортный анализ", "Прогноз LTV",
                        "Сегментация юзеров", "ETL для маркетинга", "Дашборд для финансов",
                        "Self-serve BI"),
                List.of("Перевести pipeline на dbt", "Materialized views в Postgres",
                        "Schema registry для Kafka", "Quality checks Great Expectations",
                        "Метрики продукта в Amplitude", "Дашборд retention в Metabase",
                        "Документация моделей", "SLA на данные < 1 час",
                        "Алерты по аномалиям воронки", "Audit-таблицы изменений")));

        teams.add(new Team(
                "ML/AI",
                "andrey",
                List.of("stepan", "olga"),
                List.of("Рекомендации", "Антифрод", "Семантический поиск", "Генерация описаний",
                        "Чат-бот поддержки", "Классификация обращений", "A/B-фреймворк для ML",
                        "Feature Store"),
                List.of("Собрать датасет", "Бейзлайн LightGBM", "Online-inference latency < 100ms",
                        "MLflow для трекинга", "Feature engineering pipeline", "Дрифт-детектор",
                        "Канареечный rollout модели", "Shadow-mode для новой модели",
                        "GPU-инференс на Triton", "Документация по фичам")));

        teams.add(new Team(
                "Design",
                "sofia",
                List.of("artem", "yana"),
                List.of("Ребрендинг 2026", "Иллюстрации к лендингу", "Иконпак",
                        "Гайдлайн по бренду", "Motion-дизайн анимации", "Дизайн админки",
                        "Дизайн мобильного онбординга", "UX-исследование чекаута"),
                List.of("Подобрать палитру", "Шрифтовая пара", "Иконки в Figma",
                        "Спрайт SVG", "Анимации Lottie", "Прототип чекаута",
                        "Тестирование на пользователях", "Дизайн пустых состояний",
                        "Иллюстрации к ошибкам", "Хедер и футер обновить")));

        return teams;
    }

    private void seedDemoData(User admin) {
        // Стабильный seed для воспроизводимости демо.
        Random rnd = new Random(424242L);

        // Создаём пользователей: лиды + члены команд (без дублей).
        Map<String, User> userByLogin = new LinkedHashMap<>();
        for (Team team : teams()) {
            userByLogin.computeIfAbsent(team.leadLogin(),
                    login -> userService.createUser(login, DEMO_USER_PASSWORD, Role.USER));
            for (String member : team.memberLogins()) {
                userByLogin.computeIfAbsent(member,
                        login -> userService.createUser(login, DEMO_USER_PASSWORD, Role.USER));
            }
        }

        log.info("Создано демо-юзеров: {}", userByLogin.size());

        // Личные проекты для каждого пользователя в его команде.
        // По 7+ проектов на пользователя: 6–8 базовых из шаблонов команды + 1 личный «лабораторный».
        int totalProjects = 0;
        int totalTasks = 0;
        for (Team team : teams()) {
            List<String> teamMemberLogins = new ArrayList<>();
            teamMemberLogins.add(team.leadLogin());
            teamMemberLogins.addAll(team.memberLogins());

            for (String userLogin : teamMemberLogins) {
                User owner = userByLogin.get(userLogin);
                int projectCount = 7 + rnd.nextInt(2); // 7 или 8 проектов

                for (int i = 0; i < projectCount; i++) {
                    String baseTitle = team.projectTemplates().get(i % team.projectTemplates().size());
                    String title;
                    if (i < team.projectTemplates().size()) {
                        title = baseTitle + " · " + owner.getLogin();
                    } else {
                        title = baseTitle + " v2 · " + owner.getLogin();
                    }
                    ProjectStatus pStatus = pickProjectStatus(rnd);

                    Project project = projectRepository.save(new Project(
                            title,
                            "Демо-проект команды «" + team.name() + "». Владелец: " + owner.getLogin() + ".",
                            pStatus,
                            owner));

                    // 4–6 задач на проект, статусы разбросаны (TODO/IN_PROGRESS/DONE/BLOCKED).
                    int taskCount = 4 + rnd.nextInt(3);
                    List<TaskStatus> statusRotation = balancedStatuses(taskCount, rnd);
                    for (int t = 0; t < taskCount; t++) {
                        String taskTitle = team.taskTemplates()
                                .get((i * 13 + t * 7 + rnd.nextInt(team.taskTemplates().size()))
                                        % team.taskTemplates().size());
                        TaskStatus tStatus = statusRotation.get(t);
                        LocalDate deadline = pickDeadline(rnd, tStatus);
                        User assignee = pickAssignee(rnd, teamMemberLogins, userByLogin);

                        Task task = taskRepository.save(new Task(
                                taskTitle,
                                "Задача в рамках проекта «" + title + "» (команда «" + team.name() + "»).",
                                tStatus,
                                deadline,
                                project,
                                assignee));
                        totalTasks++;

                        // На 1 задачу из 5 — пара комментариев.
                        if (rnd.nextInt(5) == 0) {
                            User commenter1 = pickAssignee(rnd, teamMemberLogins, userByLogin);
                            commentRepository.save(new Comment(
                                    pickComment(rnd, tStatus), task, commenter1));
                            if (rnd.nextInt(2) == 0) {
                                User commenter2 = pickAssignee(rnd, teamMemberLogins, userByLogin);
                                commentRepository.save(new Comment(
                                        pickComment(rnd, tStatus), task, commenter2));
                            }
                        }
                    }
                    totalProjects++;
                }
            }
        }

        // Кросс-командные проекты под админом — общие, задачи на людей из разных команд.
        if (admin != null) {
            List<User> allUsers = new ArrayList<>(userByLogin.values());
            String[] sharedTitles = {
                    "Релиз v2.0 — общий план",
                    "Q4 OKRs",
                    "Хакатон 2026",
                    "Программа надёжности и SLA",
                    "Запуск нового продукта",
                    "Технический долг — приоритет 2026"
            };
            String[] sharedTasks = {
                    "Согласовать roadmap с продактом",
                    "Подготовить демо для стейкхолдеров",
                    "Координация релизов между командами",
                    "Бюджет на квартал",
                    "Постмортем инцидента",
                    "Брейншторм фичей",
                    "Подготовить материалы для onboarding",
                    "Cross-team sync по API-контрактам",
                    "Архитектурный ревью"
            };
            for (String sharedTitle : sharedTitles) {
                Project shared = projectRepository.save(new Project(
                        sharedTitle,
                        "Кросс-командный проект под админом — задачи распределены между командами.",
                        ProjectStatus.ACTIVE,
                        admin));
                int n = 5 + rnd.nextInt(3);
                List<TaskStatus> rot = balancedStatuses(n, rnd);
                for (int t = 0; t < n; t++) {
                    User assignee = allUsers.get(rnd.nextInt(allUsers.size()));
                    Task task = taskRepository.save(new Task(
                            sharedTasks[(t * 5 + rnd.nextInt(sharedTasks.length)) % sharedTasks.length],
                            "Кросс-командная задача в проекте «" + sharedTitle + "». Исполнитель из команды любой.",
                            rot.get(t),
                            pickDeadline(rnd, rot.get(t)),
                            shared,
                            assignee));
                    totalTasks++;
                    if (rnd.nextInt(3) == 0) {
                        commentRepository.save(new Comment(
                                pickComment(rnd, rot.get(t)), task,
                                allUsers.get(rnd.nextInt(allUsers.size()))));
                    }
                }
                totalProjects++;
            }
        }

        log.info("Заведено проектов: {}, задач: {}", totalProjects, totalTasks);
    }

    /**
     * Серия отдельных транзакций: для нескольких задач делаем 1–3 «прохода» по смене статуса/дедлайна.
     * Каждый коммит — отдельная ревизия в {@code tasks_aud} (Hibernate Envers), и в UI
     * {@code /tasks/{id}/history} получится реалистичная история, а не одна запись «СОЗДАНО».
     */
    private void seedTaskHistoryRevisions(TransactionTemplate tt) {
        Random rnd = new Random(1717L);
        Long maxTaskId = taskRepository.findAll().stream()
                .map(Task::getId).max(Long::compareTo).orElse(0L);
        if (maxTaskId == 0L) {
            return;
        }

        // Берём ~15 задач — для каждой делаем 2–3 update'а в отдельных транзакциях.
        for (int i = 0; i < 15; i++) {
            long taskId = 1L + (long) rnd.nextInt(maxTaskId.intValue());
            int passes = 2 + rnd.nextInt(2);
            for (int pass = 0; pass < passes; pass++) {
                TaskStatus next = TaskStatus.values()[rnd.nextInt(TaskStatus.values().length)];
                LocalDate newDeadline = LocalDate.now().plusDays(-15 + rnd.nextInt(45));
                tt.execute(status -> {
                    taskRepository.findById(taskId).ifPresent(task -> {
                        task.setStatus(next);
                        task.setDeadline(newDeadline);
                        taskRepository.save(task);
                    });
                    return null;
                });
            }
        }
    }

    private static ProjectStatus pickProjectStatus(Random rnd) {
        int r = rnd.nextInt(10);
        if (r < 7) return ProjectStatus.ACTIVE;
        if (r < 9) return ProjectStatus.COMPLETED;
        return ProjectStatus.ARCHIVED;
    }

    /** Сбалансированный набор статусов — хотя бы по одной TODO/IN_PROGRESS/DONE, иногда BLOCKED. */
    private static List<TaskStatus> balancedStatuses(int count, Random rnd) {
        List<TaskStatus> out = new ArrayList<>(count);
        out.add(TaskStatus.TODO);
        out.add(TaskStatus.IN_PROGRESS);
        out.add(TaskStatus.DONE);
        while (out.size() < count) {
            int r = rnd.nextInt(10);
            if (r < 3) out.add(TaskStatus.TODO);
            else if (r < 6) out.add(TaskStatus.IN_PROGRESS);
            else if (r < 9) out.add(TaskStatus.DONE);
            else out.add(TaskStatus.BLOCKED);
        }
        // Перемешиваем, чтобы канбан не выглядел упорядоченно.
        for (int i = out.size() - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            TaskStatus tmp = out.get(i);
            out.set(i, out.get(j));
            out.set(j, tmp);
        }
        return out;
    }

    private static LocalDate pickDeadline(Random rnd, TaskStatus status) {
        LocalDate today = LocalDate.now();
        switch (status) {
            case DONE: return today.minusDays(1 + rnd.nextInt(30));
            case BLOCKED: return today.plusDays(rnd.nextInt(7));
            case IN_PROGRESS: return today.plusDays(2 + rnd.nextInt(14));
            case TODO:
            default: return today.plusDays(5 + rnd.nextInt(30));
        }
    }

    private static User pickAssignee(Random rnd, List<String> logins, Map<String, User> all) {
        return all.get(logins.get(rnd.nextInt(logins.size())));
    }

    private static String pickComment(Random rnd, TaskStatus status) {
        String[] generic = {
                "Беру задачу.",
                "Уточнил требования — продолжаю.",
                "Готово, прошу проверить.",
                "Сверила схему — есть пара замечаний, оставлю отдельно.",
                "Заблочено до решения с архитектором.",
                "Перенёс дедлайн — параллельно идёт связанная задача.",
                "Code review запрошен у тимлида.",
                "Покрыл тестами, прогон зелёный.",
                "Ждём подтверждения от бизнеса."
        };
        return generic[rnd.nextInt(generic.length)];
    }

    private boolean isDevProfile() {
        List<String> active = Arrays.asList(environment.getActiveProfiles());
        return active.contains("dev") || active.isEmpty(); // дефолтный профиль в Spring Boot — пустой → dev
    }
}
