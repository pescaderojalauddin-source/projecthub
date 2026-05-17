# Что сделано в проекте ProjectHub

> Полный список реализованного функционала с указанием файлов, где это живёт.
> Цель этого документа: одним взглядом понять, что вообще получилось.

## Оглавление

- [1. Базовая модель и роли](#1-базовая-модель-и-роли)
- [2. Безопасность (Spring Security)](#2-безопасность-spring-security)
- [3. Производительность и ORM](#3-производительность-и-orm)
- [4. UX и интерфейс](#4-ux-и-интерфейс)
- [5. Quick-wins v1 — Личный кабинет](#5-quick-wins-v1--личный-кабинет)
- [6. Quick-wins v2 — Геймификация и наглядность](#6-quick-wins-v2--геймификация-и-наглядность)
- [7. Quick-wins v3 — Pro-feel](#7-quick-wins-v3--pro-feel)
- [8. Quick-wins v4 — Менеджмент и коммуникация](#8-quick-wins-v4--менеджмент-и-коммуникация)
- [9. REST API и документация](#9-rest-api-и-документация)
- [10. Мониторинг и наблюдаемость](#10-мониторинг-и-наблюдаемость)
- [11. Тесты](#11-тесты)
- [12. Демо-данные](#12-демо-данные)
- [13. Деплой](#13-деплой)

---

## 1. Базовая модель и роли

Сущности и связи:

| Сущность | Поля | Связи |
| --- | --- | --- |
| `User` | login, password (BCrypt), role, email, emailNotifications | 1 → N `Project`, 1 → N `Task`, 1 → N `Comment` |
| `Project` | title, description, status, emoji, createdAt | N → 1 `User` owner; 1 → N `Task` |
| `Task` | title, description, status, priority, deadline, tags, createdAt, updatedAt, version | N → 1 `Project`, N → 1 `User` assignee, 1 → N `Comment`, 1 → N `TaskAttachment` |
| `Comment` | text, createdAt | N → 1 `Task`, N → 1 `User` author |
| `TaskAttachment` | filename, storagePath, contentType, sizeBytes, uploadedBy, uploadedAt | N → 1 `Task` |
| `ProjectStar` | userId+projectId (composite PK) | many-to-many избранного |
| `AchievementUnlocked` | userId, code, unlockedAt | список разблокированных ачивок |

**Роли:** `USER` (видит только свои проекты) и `ADMIN` (видит всё, доступ к админке и мониторингу).

**Миграции Flyway:**

- `V1__init_schema.sql` — базовая схема
- `V2__audit_columns_and_optimistic_locking.sql` — createdAt/updatedAt + @Version
- `V3__envers_audit_tables.sql` — таблицы аудита Envers
- `V4__project_stars.sql` — избранные проекты
- `V5__project_emoji_and_achievements.sql` — эмодзи проектов + ачивки
- `V6__priority_tags_attachments.sql` — приоритет, теги, вложения, email

## 2. Безопасность (Spring Security)

- **BCrypt-хэширование** паролей (`PasswordEncoder` bean).
- **CSRF включён** для всех изменяющих форм + HTMX (см. `htmx-csrf.js`).
- **Form login** с `/login` + `defaultSuccessUrl=/dashboard`.
- **URL-правила:** `/admin/**`, `/monitoring/**` → только `ADMIN`; остальное — `authenticated`.
- **`@PreAuthorize`** на сервис-уровне (защита продублирована — UI + бизнес-слой).
- **H2-console под `@Profile("dev")`** — недоступна в проде.
- **Rate limit** на `/login` и `/register`: 5 попыток в минуту с одного IP (`RateLimitingFilter`).
- **Безопасный сидинг ADMIN:** в проде требуется `PROJECTHUB_SEED_ADMIN_PASSWORD`, иначе админ не создаётся.
- **Sanitizer:** Markdown в описаниях прогоняется через jsoup Safelist (без `<script>`, `<iframe>`, `<img>`); внешние ссылки получают `target="_blank" rel="noopener noreferrer nofollow"`.
- **MentionsService** — escape входного HTML до подсветки `@user`.

## 3. Производительность и ORM

- `open-in-view=false` — рендеринг шаблонов идёт без открытой Hibernate Session.
- `@EntityGraph` для всех списочных запросов — N+1 закрыт:
  - `Project.owner` подгружается одним JOIN'ом
  - `Task.assignee`, `Task.tags`, `Task.project.owner` — оптом
- **Один SQL вместо N** для прогресс-бара: `countByProjectIdGroupByStatus(IN :ids)`.
- **`@Version`** на `Project` и `Task` — оптимистическая блокировка.
- **JPA Auditing**: `@CreatedDate`, `@LastModifiedDate` (`JpaConfig`).
- **Hibernate Envers** ведёт историю изменений задач — страница `/tasks/{id}/history`.
- **Connection pool**: HikariCP (стандартный Boot).
- **Index'ы:** `tasks(priority)`, `task_tags(tag)`, `task_attachments(task_id)`, FK всех связей.

## 4. UX и интерфейс

- **Bootstrap 5.3** + **Bootstrap Icons** + **Thymeleaf 3.1** на сервере, **HTMX** для частичных обновлений.
- **Тёмная тема** через `data-bs-theme`, переключатель в навбаре (`themeToggle`).
- **Канбан-доска** `/projects/{id}/board` с drag-drop через SortableJS, статус сохраняется HTMX-запросом.
- **История изменений задачи** `/tasks/{id}/history` — список ревизий Envers.
- **Глобальный поиск** в навбаре (HTMX live-dropdown, debounce 300мс).
- **Прогресс-бары** проектов в списке и на странице проекта.
- **Аватарки-инициалы** — детерминированный цвет по логину, без сторонних библиотек.

## 5. Quick-wins v1 — Личный кабинет

Реализованные фичи (одним пакетом, ветка `devin/quick-wins`):

| Фича | Файлы |
| --- | --- |
| Дашборд `/dashboard` с 4 счётчиками (Сегодня / Просрочено / В работе / Готово) | `DashboardController`, `templates/dashboard.html` |
| Глобальный поиск в навбаре (HTMX) | `SearchController`, `fragments/layout.html` |
| Прогресс-бар проекта (один SQL) | `ProjectProgressService` |
| Аватарки-инициалы | `fragments/avatar.html`, `app.css` |
| `/` → редирект на `/dashboard` | `HomeController` |

## 6. Quick-wins v2 — Геймификация и наглядность

Ветка `devin/quick-wins-2`:

| Фича | Файлы |
| --- | --- |
| ⭐ Избранные проекты | `ProjectStar`, `ProjectStarRepository`, `ProjectStarController`, `V4__project_stars.sql` |
| 🎉 Конфетти при DONE | `static/js/confetti.js` |
| 📊 Графики Chart.js (статусы donut + 7-дневный bar) | `dashboard.html`, `static/js/dashboard-charts.js` |
| 📅 Календарь дедлайнов проекта | `ProjectController.calendar()`, `templates/projects/calendar.html` |

## 7. Quick-wins v3 — Pro-feel

Ветка `devin/quick-wins-3`:

| Фича | Файлы |
| --- | --- |
| ⌨️ Хоткеи `/`, `g d`, `g p`, `n`, `?`, `t`, `Esc` + cheatsheet-модалка | `static/js/shortcuts.js`, `fragments/layout.html` |
| 🔔 Toast-уведомления (`ProjectHubToast.success/error/...`) | `static/js/toasts.js` |
| 🎁 Emoji-иконки проектов (28 эмодзи в пикере) | `Project.emoji`, `ProjectForm`, `V5` миграция |
| 📝 Markdown в описаниях (CommonMark + jsoup) | `MarkdownService` |
| 🏆 Ачивки (8 штук, авто-разблокировка) | `AchievementUnlocked`, `AchievementService`, `V5` миграция |

## 8. Quick-wins v4 — Менеджмент и коммуникация

Ветка `devin/quick-wins-4`. Эти 8 фич были запрошены последним сообщением:

### 8.1. 🌅 Приветствие по времени дня

«Доброе утро / день / вечер, `<login>`» на дашборде + иконка `bi-sun`/`bi-cloud-sun`/`bi-moon-stars`.
Файлы: `DashboardController.greeting()`, `templates/dashboard.html`.

### 8.2. 📋 Экспорт CSV задач проекта

Кнопка «Скачать CSV» на странице проекта → `GET /projects/{id}/tasks/export.csv`.
OpenCSV 5.9. С UTF-8 BOM (`\uFEFF`) — корректно открывается в Excel.
Файлы: `TaskExportController`, `pom.xml` (зависимость).

### 8.3. 📌 Приоритет задач

Enum `TaskPriority { LOW, MEDIUM, HIGH, URGENT }` с цветом и иконкой.
В таблице задач — колонка приоритета, в форме — select, на канбане — цветная полоска слева. Сортировка по приоритету: `?sort=priority,DESC`.
Файлы: `TaskPriority`, `Task.priority`, `TaskForm`, `templates/tasks/form.html`, `templates/projects/view.html`.

### 8.4. 🏷️ Теги задач

Many-to-many через side-таблицу `task_tags(task_id, tag)`, `@ElementCollection`.
В форме — строка через запятую (`tagsCsv`), парсится → lower-case, dedup. На карточках — чипы.
Фильтр по тегу: `/projects/{id}?tag=backend` (HTMX).
Файлы: `Task.tags`, `TaskService.parseTags`, `TaskForm.tagsCsv`, `templates/tasks/form.html`.

### 8.5. 📊 Burndown-график

Линия Chart.js за последние 30 дней: «осталось открыто» (sync) и «сделано» (накопительная).
Считается одним SQL по `tasks.updated_at` + `tasks.created_at`.
Файлы: `BurndownService`, `static/js/burndown.js`, `templates/projects/view.html`.

### 8.6. 💬 @-меншены в комментариях

Регулярка `@login` → подсветка в HTML + клик на `/profile/{login}`.
Кэш логинов на 30 секунд (volatile + nano-timestamp).
Автодополнение при наборе `@` в textarea (↑↓ навигация, Enter/Tab вставка).
Файлы: `MentionsService`, `static/js/mentions.js`, `templates/tasks/view.html`.

### 8.7. 📎 Вложения файлов

Drag-drop файлов на задачу. Хранение на диске в `uploads/` (путь: `PROJECTHUB_UPLOADS_DIR`).
Метаданные в `task_attachments`. Скачивание `GET /tasks/{taskId}/attachments/{attId}/download`.
RBAC: удалить может загрузивший, владелец проекта или ADMIN.
Файлы: `TaskAttachment`, `TaskAttachmentRepository`, `TaskAttachmentService`, `TaskAttachmentController`, `static/js/attachments.js`.

### 8.8. 🔔 Email-уведомления (утренний дайджест)

Spring Mail + `@Scheduled(cron = "0 0 8 * * *")` — каждое утро в 08:00 шлёт пользователям с `emailNotifications=true` список просроченных задач.
Если `JavaMailSender` не сконфигурирован — пишет письмо в лог (`[email-dry-run]`) для отладки.
Страница `/settings` — пользователь сам выставляет email и подписку. Админ может нажать «Запустить дайджест сейчас» — `POST /admin/notifications/run`.
Файлы: `EmailNotificationService`, `SettingsController`, `templates/settings/view.html`, `application.yml`.

## 9. REST API и документация

- HTTP Basic, JSON, под `/api/v1/**`.
- 16 эндпоинтов (CRUD проектов, задач, комментариев + админка).
- OpenAPI 3.0 через **springdoc-openapi 2.8.6**, Swagger UI на `/swagger-ui.html`.
- DTO-слой (`UserDto`, `ProjectDto`, `TaskDto`, …) — не отдаём JPA-сущности наружу.
- Глобальный `@RestControllerAdvice` для JSON-ошибок 400/403/404/409/500.

Полный список маршрутов — в `README.md`.

## 10. Мониторинг и наблюдаемость

- **Spring Boot Actuator**: `/actuator/health`, `/actuator/info` — публично.
- **JavaMelody**: `/monitoring` — только `ADMIN`. Графики JVM, SQL, HTTP, кэшей.
- **Логи**: Logback, `application.yml` уровень `INFO`, debug-режим для security в дев-профиле.

## 11. Тесты

После запуска `./mvnw verify`:

```
35 unit-тестов (services, security, init)
50 интеграционных тестов (web/api)
 2 PostgreSQL-теста (skipped без Docker)
```

**Покрытие фич:** `QuickWinsIT.java` — 25 тестов: greeting, CSV, priority, tags, burndown, mentions, attachments, settings, эмодзи, markdown, ачивки, поиск, прогресс-бар, избранное, дашборд.

**Прочее:** `AuthFlowIT`, `RestApiIT`, `UxFeaturesIT`, `RateLimitingFilterTest`, `ProjectAuditingAndLockingTest`, `DataLoaderTest`.

## 12. Демо-данные

`DataLoader` сидирует богатый набор при первом запуске на пустую БД:

- **27 пользователей** (1 ADMIN + 26 USER), всем выставлен email `<login>@example.com`.
- **8 команд:** Backend / Frontend / Mobile / DevOps / QA / Data / ML / Design + кросс-командные проекты под ADMIN.
- **~203 проекта** — минимум 7 на пользователя, тематические эмодзи.
- **~1030 задач** в 4 статусах (TODO / IN_PROGRESS / DONE / BLOCKED), разные приоритеты, теги, дедлайны (в прошлом/будущем для разнообразия), 15 задач — с 2-3 ревизиями Envers.
- **~337 комментариев** с разной тональностью.

Пароли: `admin/admin123` и `user123` для всех остальных. Полный список логинов — в README.md.

## 13. Деплой

В корне репо:

- `Dockerfile` (multi-stage, distroless)
- `docker-compose.yml` (приложение + Postgres)
- `render.yaml` (Blueprint для 1-click деплоя на Render.com)

Подробная инструкция — в `DEPLOYMENT.md`: Render, Fly.io, Railway, VPS с пошаговыми скриншотами команд.
