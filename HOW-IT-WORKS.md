# Как устроен ProjectHub

> Документ-«экскурсия» по архитектуре и реализации. Если ты открыл репо впервые
> и хочешь понять, куда смотреть и почему оно вообще работает — читай по порядку.

## Оглавление

- [1. Архитектура «с высоты птичьего полёта»](#1-архитектура-с-высоты-птичьего-полёта)
- [2. Структура пакетов](#2-структура-пакетов)
- [3. Слой контроллеров](#3-слой-контроллеров)
- [4. Слой сервисов](#4-слой-сервисов)
- [5. Слой репозиториев / ORM](#5-слой-репозиториев--orm)
- [6. Шаблоны Thymeleaf + HTMX](#6-шаблоны-thymeleaf--htmx)
- [7. Безопасность](#7-безопасность)
- [8. Миграции БД (Flyway)](#8-миграции-бд-flyway)
- [9. Как работают конкретные фичи](#9-как-работают-конкретные-фичи)
- [10. Конфигурация через env](#10-конфигурация-через-env)
- [11. Как добавить новую фичу — рецепт](#11-как-добавить-новую-фичу--рецепт)

---

## 1. Архитектура «с высоты птичьего полёта»

Классическая трёхслойка Spring Boot MVC + ORM:

```
┌──────────────────────────────────────────────────────────────────┐
│  Browser  ←─── HTML/HTMX/JSON ───→  Spring DispatcherServlet     │
│                                            │                     │
│                                            ▼                     │
│                                  ┌──────────────────┐            │
│                                  │  @Controller     │  ← MVC     │
│                                  │  @RestController │  ← REST    │
│                                  └────────┬─────────┘            │
│                                           │ DTO / Form           │
│                                           ▼                      │
│                                  ┌──────────────────┐            │
│                                  │   @Service       │            │
│                                  │  (бизнес-логика, │            │
│                                  │  @Transactional) │            │
│                                  └────────┬─────────┘            │
│                                           │ Entity               │
│                                           ▼                      │
│                                  ┌──────────────────┐            │
│                                  │ Spring Data JPA  │            │
│                                  │  (Repositories)  │            │
│                                  └────────┬─────────┘            │
│                                           │ JPA / Hibernate      │
│                                           ▼                      │
│                                  ┌──────────────────┐            │
│                                  │  H2 (dev/test)   │            │
│                                  │  Postgres (prod) │            │
│                                  └──────────────────┘            │
└──────────────────────────────────────────────────────────────────┘
```

Ключевые «над-слоевые» компоненты:

- **Spring Security** перехватывает каждый запрос до DispatcherServlet, проверяет роли и CSRF.
- **JavaMelody filter** меряет каждую операцию (HTTP + SQL).
- **Flyway** при старте проверяет/применяет миграции.
- **JPA Auditing** автоматически проставляет `createdAt`/`updatedAt`.
- **Hibernate Envers** пишет ревизии в таблицы `*_aud`.

## 2. Структура пакетов

```
com.example.projecthub/
├── ProjecthubApplication.java       — main + @SpringBootApplication
├── config/
│   ├── SecurityConfig.java          — фильтры, /api basic, form login, @Profile("dev") для H2
│   ├── JpaConfig.java               — @EnableJpaAuditing
│   ├── AsyncConfig.java             — @EnableAsync + @EnableScheduling, пул потоков
│   └── EnversConfig.java            — настройка Envers
├── controller/
│   ├── ProjectController.java       — /projects/**  (HTML)
│   ├── TaskController.java          — /tasks/**, /projects/{id}/tasks/**
│   ├── DashboardController.java     — /, /dashboard
│   ├── SearchController.java        — глобальный поиск (HTMX)
│   ├── TaskExportController.java    — CSV-экспорт
│   ├── TaskAttachmentController.java— upload/download/delete
│   ├── SettingsController.java      — /settings + /admin/notifications/run
│   ├── ProjectStarController.java   — toggle ⭐
│   └── api/
│       ├── ProjectRestController.java
│       ├── TaskRestController.java
│       ├── CommentRestController.java
│       └── AdminRestController.java
├── service/
│   ├── ProjectService.java, TaskService.java, UserService.java
│   ├── ProjectProgressService.java  — прогресс-бар (1 SQL)
│   ├── BurndownService.java         — burndown-серии
│   ├── MentionsService.java         — рендер @-меншенов
│   ├── MarkdownService.java         — Markdown + sanitiser
│   ├── AchievementService.java      — ачивки
│   ├── EmailNotificationService.java— @Scheduled дайджест
│   ├── TaskAttachmentService.java   — файлы
│   └── CurrentUserService.java      — единая точка получения текущего юзера
├── repository/
│   ├── UserRepository.java, ProjectRepository.java, TaskRepository.java
│   ├── CommentRepository.java, ProjectStarRepository.java
│   ├── TaskAttachmentRepository.java
│   └── AchievementUnlockedRepository.java
├── entity/  — JPA-сущности
├── dto/     — DTO для форм и REST
├── security/— RateLimitingFilter, кастомные обработчики
├── init/    — DataLoader (сидинг при пустой БД)
└── exception/ — ResourceNotFoundException, ControllerAdvice
```

## 3. Слой контроллеров

Два типа:

- **`@Controller`** — возвращает имя Thymeleaf-шаблона (`projects/view`, `dashboard`, …) или `redirect:/…`.
- **`@RestController`** — возвращает JSON (DTO). Лежит в `controller/api/`.

Правила:

- Контроллер не знает про JPA-сущности «снаружи» — только через сервис.
- `RedirectAttributes.addFlashAttribute("flashSuccess", …)` для one-time-сообщений после POST.
- HTMX-эндпоинты возвращают фрагмент шаблона (`templates/fragments/...`).
- CSRF-токен в формах подкладывается через `<input type="hidden" th:if="${_csrf}" …>`.

## 4. Слой сервисов

- Все сервисы аннотированы `@Service @Transactional` (readOnly=false по умолчанию).
- Только сервис вызывает репозиторий. Контроллер вызывает только сервис.
- Бизнес-правила (RBAC, валидация связей) — в сервисе, продублированы `@PreAuthorize` где есть смысл.
- Сервисы возвращают **JPA-сущности** или DTO. Для REST контроллер потом конвертит в DTO.
- Конструкторное внедрение — никаких `@Autowired` на полях.

Пример (TaskService):

```java
@Service
@Transactional
public class TaskService {
    private final TaskRepository repo;
    private final ProjectService projectService;

    public TaskService(TaskRepository repo, ProjectService projectService) {
        this.repo = repo;
        this.projectService = projectService;
    }

    public Task create(Long projectId, TaskForm form, User actor) {
        Project p = projectService.getByIdForUser(projectId, actor);
        // RBAC уже проверен внутри getByIdForUser
        Task t = new Task(form.getTitle(), …);
        t.setTags(parseTags(form.getTagsCsv()));
        return repo.save(t);
    }
}
```

## 5. Слой репозиториев / ORM

- Используем **Spring Data JPA** репозитории — большинство методов derived (`findAllByProject…`), для GROUP BY/JOIN — `@Query`.
- **Везде `@EntityGraph`** для агрегации связей: задачи на странице проекта тянут `assignee` и `tags` одним SELECT, без N+1.
- **`open-in-view=false`** — после закрытия транзакции ленивые поля уже недоступны. Поэтому всё, что нужно в шаблоне, тянется через graph.
- **`@Version`** на `Project`/`Task` — параллельное редактирование вызовет `ObjectOptimisticLockingFailureException`.
- **JpaConfig** включает `@EnableJpaAuditing` для `@CreatedDate`/`@LastModifiedDate`.
- **EnversConfig + `@Audited`** на `Task`, `Project` — ревизии пишутся в `tasks_aud`/`projects_aud`. Страница `/tasks/{id}/history` показывает через `RevisionRepository`.

## 6. Шаблоны Thymeleaf + HTMX

- Базовый layout: `templates/fragments/layout.html` (head, navbar, footer, scripts).
- Каждая страница: `<head th:replace="~{fragments/layout :: head('Title')}">…`
- Динамика без полной перезагрузки — **HTMX**: `hx-post`, `hx-target`, `hx-swap`. Примеры:
  - `POST /tasks/{id}/status` → возвращает обновлённую карточку для канбана;
  - `GET /search` → возвращает фрагмент `fragments/search-results`;
  - `POST /projects/{id}/star` → возвращает обновлённую кнопку-звёздочку.
- **CSRF + HTMX:** `htmx-csrf.js` подмешивает токен из meta-тега в каждый небезопасный запрос.
- **Theming:** `data-bs-theme="dark|light"` на `<html>`, переключатель в навбаре, состояние в `localStorage`.

## 7. Безопасность

`SecurityConfig` объявляет **две цепочки**:

1. **API-цепочка** (`/api/**`) — HTTP Basic, без CSRF (REST stateless).
2. **Web-цепочка** (всё остальное) — form-login на `/login`, CSRF включён, sessions on.

Дополнительно:

- `@Profile("dev")` цепочка для H2-консоли (`/h2-console/**`, `frameOptions=sameOrigin`).
- `RateLimitingFilter` (`@Order(Ordered.HIGHEST_PRECEDENCE)`) — 5 попыток `/login` или `/register` в минуту с одного IP.
- Пароль ADMIN не зашит — `PROJECTHUB_SEED_ADMIN_PASSWORD` или fallback `admin123` в dev-профиле.
- BCrypt strength=10 (стандартный Spring).
- `@PreAuthorize("hasRole('ADMIN')")` на бизнес-методах (`UserService.changeRole`, …) — второй уровень защиты.

## 8. Миграции БД (Flyway)

- Все DDL живут в `src/main/resources/db/migration`.
- Имена `V<номер>__<описание>.sql`. Версии монотонно растут.
- Flyway применяется на старте автоматически (`spring.flyway.enabled=true`).
- В тестах используем H2 в режиме совместимости с PostgreSQL.

## 9. Как работают конкретные фичи

### 9.1. Дашборд `/dashboard`

`DashboardController.dashboard(...)`:

1. Через `CurrentUserService` берём текущего пользователя.
2. Делаем 4 SQL-запроса (`countByAssignee...`) для счётчиков + 4 списка `findTop10ByAssignee...`.
3. Считаем donut по статусам и bar за 7 дней через два `@Query`.
4. `AchievementService.evaluateAndUnlockNew(user)` проверяет 8 условий и возвращает разблокированное.
5. По времени дня (LocalTime) выбираем приветствие и иконку.
6. Прокидываем всё в `templates/dashboard.html`.

Шаблон рисует карточки + Chart.js + ачивки + конфетти-trigger через `flashConfetti`.

### 9.2. Канбан с drag-drop

`templates/projects/board.html` рисует 4 колонки. **SortableJS** на каждой `.kanban-list`:

```js
new Sortable(list, {
    group: 'kanban',
    onAdd: (evt) => htmx.ajax('POST', `/tasks/${id}/status?status=${target}`, …)
});
```

Бэкенд: `TaskController.updateStatus(...)` → `TaskService.changeStatus(...)` → возвращает заново отрендеренный фрагмент карточки.

Если статус стал `DONE` — JS-stub запускает `window.fireConfetti(rect)`.

### 9.3. Поиск

`SearchController.search(@RequestParam String q, Principal p)`:

1. Если строка короче 2 символов — возвращаем фрагмент-хинт.
2. Иначе — `projectRepository.searchScope(p, q)` + `taskRepository.searchScope(p, q)` (ADMIN получает всё, USER — только свои).
3. Возвращаем `fragments/search-results`. HTMX подставляет в dropdown.

### 9.4. CSV-экспорт

`TaskExportController.exportCsv(...)` пишет UTF-8 BOM + CSV через **OpenCSV** `CSVWriter`. Заголовок: `id;title;status;priority;assignee;deadline;tags`. Возвращает `text/csv; charset=UTF-8` + `Content-Disposition: attachment; filename*=UTF-8''…`.

### 9.5. Burndown

`BurndownService.compute(project)`:

1. Берём дату 30 дней назад.
2. Один SQL: `SELECT created_at, status FROM tasks WHERE project_id=:p`.
3. Идём по дням и считаем «сколько задач было открыто на конец дня» и «сколько суммарно DONE».
4. Возвращаем 3 списка: labels, openCounts, doneCounts.

В шаблоне `<canvas data-labels="…|…">`. JS (`burndown.js`) парсит `split('|')` и строит линию Chart.js.

### 9.6. @-меншены

`MentionsService.render(text)`:

1. Escape HTML.
2. Regex `(?<![\p{L}\p{N}_])@([A-Za-z0-9._-]{2,64})`.
3. Для каждого матча: если логин есть в `cachedLogins` (TTL 30 сек) — оборачиваем в `<a class="mention" href="/profile/login">@login</a>`.
4. `\n` → `<br/>`.

Шаблон `templates/tasks/view.html`:

```html
<div class="mt-1 comment-body" th:utext="${@mentionsService.render(c.text)}"></div>
```

Клиентская часть (`mentions.js`) при наборе `@` показывает dropdown с автодополнением, читает логины из `textarea[data-logins]`.

### 9.7. Вложения

Хранение на диске:

- Папка задаётся в `application.yml`: `projecthub.uploads.dir = ${PROJECTHUB_UPLOADS_DIR:uploads}`.
- При сохранении: `<uuid>-<sanitized-filename>`. Запись в БД: `task_attachments(filename, storage_path, content_type, size_bytes, uploaded_by, uploaded_at)`.
- Скачивание: `FileSystemResource` + `Content-Disposition: attachment; filename*=UTF-8''…`.
- Удалить может: загрузивший, владелец проекта, или ADMIN.
- Max size = 10MB (`spring.servlet.multipart.max-file-size`).

UI: drag-drop поверх обычной формы с `<input type="file">`. JS добавляет класс `.drag-over` при наведении, на drop передаёт файлы в input и сабмитит форму.

### 9.8. Email-дайджест

```yaml
projecthub:
  notifications:
    email:
      enabled: false               # включи в проде
      from: no-reply@projecthub.local
      cron: "0 0 8 * * *"          # каждый день в 08:00
spring:
  mail:
    host: ${SMTP_HOST:localhost}
    port: ${SMTP_PORT:1025}
    username: ${SMTP_USERNAME:}
    password: ${SMTP_PASSWORD:}
```

`EmailNotificationService.sendDailyDigest()` (см. `@Scheduled(cron = …)`):

1. Если `enabled=false` — выход.
2. Для каждого пользователя с `email != null && emailNotifications`:
   - находим до 10 просроченных задач (`findTop10ByAssigneeAndDeadlineBefore...`),
   - если их нет — пропускаем,
   - формируем `SimpleMailMessage` и шлём через `JavaMailSender`.
3. Если `JavaMailSender` не сконфигурирован — пишем в лог как `[email-dry-run]`. Это позволяет демонстрировать фичу локально без SMTP.

Дополнительно `POST /admin/notifications/run` (ADMIN) — запускает `runOnce()` немедленно.

### 9.9. Markdown с sanitiser

`MarkdownService.render(text)`:

1. `MarkdownParser` (CommonMark + AutolinkExtension) → AST.
2. `HtmlRenderer` → HTML.
3. **jsoup `Cleaner` + кастомный `Safelist`**: разрешены `h1-h6`, `p`, `ul/ol/li`, `strong`/`em`, `code`/`pre`, `blockquote`, `a[href]`. Удалены `<script>`, `<iframe>`, `<img>`, on-handlers.
4. Внешним `<a href>` добавляем `target="_blank" rel="noopener noreferrer nofollow"`.

В шаблонах: `th:utext="${@markdownService.render(project.description)}"`.

### 9.10. Ачивки

`AchievementService.evaluateAndUnlockNew(user)`:

1. Для каждой из 8 ачивок проверяем условие SQL-запросом (например, `countByAssigneeAndStatus(user, DONE) >= 10`).
2. Если условие выполнено и записи в `achievements_unlocked` ещё нет — добавляем.
3. Возвращаем список новых разблокированных кодов.

Дашборд кладёт в model `achievements` (всё 8) и `achievementsUnlocked` (Set<кодов>). Шаблон рисует серые/цветные плашки, JS показывает toast по `newlyUnlocked`.

## 10. Конфигурация через env

`application.yml` использует переменные окружения с дефолтами. Ключевые:

| Переменная | Назначение | По умолчанию |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` / `prod` / `postgres` | (none, т.е. default) |
| `SPRING_DATASOURCE_URL` | JDBC URL Postgres'а | (H2 in-memory) |
| `SPRING_DATASOURCE_USERNAME`/`PASSWORD` | креды БД | (none) |
| `PROJECTHUB_SEED_ADMIN_PASSWORD` | пароль для сидинга админа в проде | (без него админ не создаётся) |
| `PROJECTHUB_UPLOADS_DIR` | путь к папке вложений | `uploads` |
| `PROJECTHUB_EMAIL_ENABLED` | включить email-дайджест | `false` |
| `PROJECTHUB_EMAIL_FROM` | from-адрес писем | `no-reply@projecthub.local` |
| `PROJECTHUB_EMAIL_CRON` | расписание дайджеста | `0 0 8 * * *` |
| `SMTP_HOST` / `SMTP_PORT` / `SMTP_USERNAME` / `SMTP_PASSWORD` | SMTP | `localhost:1025` |
| `SMTP_AUTH` / `SMTP_STARTTLS` | флаги | `false` / `false` |
| `PORT` | порт HTTP | `8080` |

В Render/Fly/Railway эти переменные задаются через UI.

## 11. Как добавить новую фичу — рецепт

1. **Сущность**: если нужна новая таблица — миграция `V7__….sql` + `@Entity` класс.
2. **Repository**: интерфейс `extends JpaRepository<…, Long>`, методы derived или `@Query`. **Не забываем `@EntityGraph`** для коллекций.
3. **Service**: `@Service @Transactional`, RBAC внутри метода (`if !owner && !admin throw 403`).
4. **DTO** (если нужен на форму или в REST): `dto/`.
5. **Controller**: `@Controller` для HTML или `@RestController` для JSON. Конструкторное внедрение. Для HTMX-ответов возвращаем `fragments/…`.
6. **Шаблон**: новый или обновлённый `.html` в `templates/`. Layout — через `th:replace`.
7. **JS/CSS** (если есть динамика): `static/js/<feature>.js`, `static/css/app.css`.
8. **Тесты**:
   - unit на сервис (Mockito);
   - integration на контроллер (MockMvc + `@SpringBootTest`).
9. **Документация**: добавь в `WHATS-DONE.md` и обнови этот файл если архитектура поменялась.

---

Если что-то непонятно — в коде Javadoc'и на каждом сервисе и контроллере поясняют намерения и инварианты.
