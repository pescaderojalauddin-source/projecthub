# Отчёт по тестированию ProjectHub

> Запуск: `./mvnw clean verify` от 2026-05-19 18:12 UTC
> Java: 17 · Spring Boot 3.5.0 · JUnit 5 · Mockito · Spring MockMvc · Testcontainers
> Результат: **BUILD SUCCESS** · **136 тестов**, 0 failures, 0 errors, 2 skipped (PostgreSQL Testcontainers — нет Docker в CI-окружении)

---

## Сводка

| Категория        | Тестов | Pass | Fail | Error | Skip |
|------------------|-------:|-----:|-----:|------:|-----:|
| Unit             |     86 |   86 |    0 |     0 |    0 |
| Integration (IT) |     50 |   48 |    0 |     0 |    2 |
| **Всего**        | **136**| **134** | **0** | **0** | **2** |

Skipped — `PostgresContainerIT` (2 теста), требуют Docker. Запускаются автоматически при наличии Docker-демона; в обычном `./mvnw verify` на CI без Docker — пропускаются.

---

## Unit-тесты (86)

Покрывают чистую бизнес-логику без HTTP и Spring-контекста (либо с минимальным `@DataJpaTest`).

| Класс | Тестов | Что покрывает |
|-------|------:|---------------|
| `ProjecthubApplicationTests` | 1 | контекст спринга поднимается |
| `init.DataLoaderTest` | 5 | сидер демо-данных: 27 юзеров, 8 проектов, ачивки, эмодзи |
| `repository.ProjectAuditingAndLockingTest` | 4 | JPA-auditing (`createdBy`/`updatedBy`), `@Version` optimistic-lock |
| `security.RateLimitingFilterTest` | 6 | rate-limit на `/login` и `/register`, 429 после превышения |
| `service.ProjectServiceTest` | 8 | CRUD + RBAC (USER видит своё, ADMIN — всё) |
| `service.TaskServiceTest` | 5 | CRUD + RBAC + смена статуса |
| `service.UserServiceTest` | 6 | регистрация, BCrypt-хеширование, ENUM ролей |
| `service.AchievementServiceTest` | 9 | 8 ачивок, разблокировка по порогам, идемпотентность |
| `service.BurndownServiceTest` | 5 | 30-дневная серия, корректные счётчики open/done |
| `service.EmailNotificationServiceTest` | 6 | dry-run без SMTP, фильтрация неотписавшихся, реальный `JavaMailSender` |
| `service.MarkdownServiceTest` | 11 | заголовки, жирный/курсив, `code`, ссылки с `target=_blank`, sanitize XSS |
| `service.MentionsServiceTest` | 9 | `@login` → ссылка, неизвестный логин → текст, HTML-escape, кэш |
| `service.ProjectStarServiceTest` | 3 | toggle ⭐ (добавляет/убирает), счётчик |
| `service.TaskTagsParseTest` | 8 | разбор CSV-тегов, lowercase, dedup, ограничение длины, `#` срезается |

### Детально: новые юнит-тесты (этой итерации)

| Файл | Тестов | Главные кейсы |
|------|-------:|---------------|
| `AchievementServiceTest.java`         | 9 | каталог 8 ачивок, `FIRST_DONE` при первой задаче, `TEN_DONE` при 10, `BUSY_BEE` при 5 in-progress, `NO_BLOCKERS` требует ≥5 задач, идемпотентность повторного evaluate |
| `BurndownServiceTest.java`            | 5 | 30 точек серии, пустой проект → нули, задача созданная сегодня не появляется на «вчерашних» днях |
| `EmailNotificationServiceTest.java`   | 6 | `enabled=false` → ничего не делает, dry-run без `JavaMailSender`, отписавшиеся пропускаются, пользователи без email пропускаются, реальный sender получает `SimpleMailMessage` |
| `MarkdownServiceTest.java`            | 11 | `<script>`/`<iframe>`/`<img>` режутся sanitizer'ом, `onclick`-handler удаляется, autolink из голой URL, ` ```fence``` ` → `<pre><code>` |
| `MentionsServiceTest.java`            | 9 | case-insensitive (`@IVAN` = `@ivan`), `foo@bar` (email) НЕ матчится из-за lookbehind, `<script>` экранируется, переводы строк → `<br/>`, инвалидация кэша |
| `ProjectStarServiceTest.java`         | 3 | toggle добавляет/удаляет, счётчик делегируется в репо |
| `TaskTagsParseTest.java`              | 8 | `null`/blank → пустой Set, lowercase + dedup, `#hashtag` обрезка `#`, 40-char truncation, `LinkedHashSet` сохраняет порядок вставки |

---

## Integration-тесты (50)

End-to-end через `@SpringBootTest(webEnvironment = MOCK)` + MockMvc. Гоняют HTTP-запросы, проверяют редиректы, статусы, содержимое HTML, CSRF, RBAC и БД-эффекты.

| Класс | Тестов | Что покрывает |
|-------|------:|---------------|
| `web.AuthFlowIT`         | 6  | login/logout, неверный пароль, регистрация, доступ к защищённым URL без аутентификации, redirect на `/dashboard` после успешного логина |
| `web.RestApiIT`          | 8  | REST `/api/projects/**` — CRUD, role checks (USER vs ADMIN), validation 400, 404 на несуществующих id |
| `web.UxFeaturesIT`       | 9  | канбан HTMX drag-drop, тёмная тема, история задач (Envers), статистика, профиль |
| `web.QuickWinsIT`        | 25 | звёздочки, конфетти-флаг, дашборд (карточки + графики Chart.js), глобальный поиск, прогресс-бары проектов, аватарки, календарь, хоткеи/cheatsheet, toasts, эмодзи проектов, markdown render + sanitize, ачивки, приоритет задач, теги, CSV-экспорт, burndown, @-меншены, drag-drop вложений, email-настройки |
| `postgres.PostgresContainerIT` | 2 | Testcontainers + Postgres 16: миграции Flyway V1–V6 применяются, DataLoader сидит данные (skipped без Docker) |

### Срез по областям функциональности

| Область | IT-тестов |
|--------|----------:|
| Авторизация / Security | 6 |
| REST API | 8 |
| UX (HTMX/канбан/тема/история) | 9 |
| Quick-Wins v1 (дашборд, поиск, прогресс, аватарки) | 5 |
| Quick-Wins v2 (звёздочки, конфетти, графики, календарь) | 5 |
| Quick-Wins v3 (хоткеи, toasts, эмодзи, markdown, ачивки) | 5 |
| Quick-Wins v4 (приоритет, теги, CSV, burndown, @-меншены, вложения, email) | 10 |
| Postgres Testcontainers (skipped без Docker) | 2 |

---

## Времена выполнения

| Этап | Время |
|------|------:|
| Unit-тесты (surefire) | ≈ 28 c |
| IT-тесты (failsafe)   | ≈ 21 c |
| Сборка + jar          | ≈ 22 c |
| **Полный `clean verify`** | **≈ 71 c** |

Самые тяжёлые тесты:
- `RestApiIT` — 12.66 c (поднимает контекст SpringBoot)
- `ProjectAuditingAndLockingTest` — 7.64 c (`@DataJpaTest` + Hibernate Envers)
- `DataLoaderTest` — 6.84 c (сид 27 пользователей + миграции H2)
- `QuickWinsIT` — 3.95 c на 25 кейсов

---

## Покрытие

Все основные сервисы покрыты unit-тестами:

```
service/
├── AchievementService         ← 9 unit-тестов
├── BurndownService            ← 5 unit-тестов
├── EmailNotificationService   ← 6 unit-тестов
├── MarkdownService            ← 11 unit-тестов
├── MentionsService            ← 9 unit-тестов
├── ProjectService             ← 8 unit-тестов
├── ProjectStarService         ← 3 unit-теста
├── TaskService (+ parseTags)  ← 5 + 8 unit-тестов
├── UserService                ← 6 unit-тестов
├── ProjectProgressService     ← покрыт через UxFeaturesIT + QuickWinsIT
├── DeadlineCalendarService    ← покрыт через QuickWinsIT (календарь)
├── StatisticsService          ← покрыт через UxFeaturesIT
├── CurrentUserService         ← покрыт через все IT-тесты
├── TaskAttachmentService      ← покрыт через QuickWinsIT
└── CommentService             ← покрыт через UxFeaturesIT
```

---

## Команды

```bash
# Полный прогон — unit + IT
./mvnw clean verify

# Только unit-тесты, быстро
./mvnw test

# Только один тест-класс
./mvnw test -Dtest=AchievementServiceTest

# Только IT-тесты
./mvnw verify -DskipTests=true -DskipITs=false

# С PostgreSQL Testcontainers (нужен Docker)
./mvnw verify -DforcePostgres=true
```

---

## Что НЕ покрыто (осознанно)

| Что | Почему |
|-----|--------|
| `application.properties` smtp-конфиг | реальный SMTP в CI — оверхед, dry-run проверяется |
| Чаcть Thymeleaf-фрагментов (footer/cheatsheet) | визуальные, проверяются вручную / на видео |
| JavaMelody-эндпоинт `/monitoring` | внешний модуль, рендерится из `monitoring.html` |
| Frontend-конфетти JS | canvas-анимация, нет смысла гонять Selenium |

---

## Финальный статус

```
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Tests run: 86, Failures: 0, Errors: 0, Skipped: 0          (unit)
[WARNING] Tests run: 50, Failures: 0, Errors: 0, Skipped: 2       (integration)
```

**Итого 136 тестов · 0 провалов · готов к защите.**
