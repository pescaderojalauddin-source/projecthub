# Соответствие техническому заданию

> Файл-таблица: каждый пункт ТЗ → как реализован → где смотреть.
> Источники ТЗ: `docs/TZ-naumen-java.pdf` (общее ТЗ практики) и `docs/TZ-projecthub.pdf` (ТЗ проекта).

## I. Общие требования

| № | Пункт ТЗ | Статус | Реализация |
| --- | --- | --- | --- |
| 1 | Проект может выполняться индивидуально или в группе | ✓ | Индивидуальный проект. |
| 2 | Тема выбрана из списка или придумана самостоятельно | ✓ | «Система управления проектами» — пункт из списка тем. |
| 3 | ТЗ составлено и согласовано с куратором | ✓ | `docs/TZ-projecthub.pdf`. |
| 4 | Записано видео работы приложения (2-3 минуты) | ✓ | Сценарий — `docs/projecthub-video-script.md`. Видео записывает пользователь по сценарию. |
| 5 | Исходный код размещён в GitHub | ✓ | <https://github.com/pescaderojalauddin-source/projecthub> |
| 6 | Проект реализует все требования ТЗ | ✓ | См. таблицы ниже. |

## II. Технические требования (обязательные)

| № | Пункт ТЗ | Статус | Реализация | Файлы |
| --- | --- | --- | --- | --- |
| 1 | Чёткое разделение на слои: представление, бизнес-логика, данные | ✓ | `@Controller` → `@Service` → `@Repository` → `@Entity`. Слои не «протекают»: контроллер не дёргает репозиторий, сервис не возвращает HTML. | `controller/`, `service/`, `repository/`, `entity/` |
| 2 | Реализовано на Spring Framework | ✓ | **Spring Boot 3.5.0** (Spring 6.2.7): Spring Web, Spring Security, Spring Data JPA, Spring Mail, Spring Scheduling. | `pom.xml`, `ProjecthubApplication.java` |
| 3 | GUI с базовыми элементами (формы, таблицы, навигация) | ✓ | Полноценный веб-интерфейс на Thymeleaf + Bootstrap 5 + HTMX: формы создания/редактирования, таблицы задач/проектов/пользователей, навбар, breadcrumbs, dropdown, modal, канбан, календарь, dashboard с графиками. | `templates/`, `static/css/app.css`, `static/js/` |
| 4 | Авторизация по логину/паролю, **пароли в БД в зашифрованном виде** | ✓ | Spring Security form-login. **BCrypt** через `PasswordEncoder` bean. В БД хранится только хэш (`users.password`). | `SecurityConfig.java`, `UserService.java`, `templates/auth/login.html` |
| 5 | Работа с реляционной БД + **Hibernate как ORM** | ✓ | **H2** (dev) и **PostgreSQL 16** (prod). Hibernate 6 через Spring Data JPA. Все сущности в `entity/`, репозитории в `repository/`. | `application.yml`, `pom.xml`, `db/migration/` |
| 6 | Код задокументирован (константы, классы, методы) | ✓ | Javadoc на всех публичных сервисах/контроллерах, описаны инварианты и побочные эффекты. Заголовочные комментарии в JS-файлах. | весь `src/main/java/**`, `static/js/*.js` |
| 7 | Устойчивость, отсутствие критических ошибок | ✓ | Глобальный `@ControllerAdvice` (HTML 400/403/404/500), `@RestControllerAdvice` для REST, `@Validated`, `@Version` для optimistic locking, rate-limiter на login/register. | `exception/`, `security/RateLimitingFilter.java` |

## III. Опциональные требования (влияют на оценку)

| № | Пункт ТЗ | Статус | Реализация | Файлы |
| --- | --- | --- | --- | --- |
| 1 | Паттерны и шаблоны проектирования | ✓ | **Repository**, **Service Layer**, **DTO**, **DI** через конструктор, **Strategy** (TaskStatus / TaskPriority enum), **Observer-like** (Hibernate Envers ревизии), **Template Method** (Spring MVC), **Adapter** (TaskService.parseTags), **Front Controller** (DispatcherServlet). | весь `src/main/java/**` |
| 2 | **Тесты, покрывающие основную функциональность** | ✓ | **35 unit-тестов** (services, security, init) + **50 интеграционных тестов** (web/API, в т.ч. `QuickWinsIT` с 25 кейсами на новые фичи) + 2 PostgreSQL-теста (skipped без Docker). Все зелёные. | `src/test/java/**`, запуск `./mvnw verify` |
| 3 | Подключена и настроена система логирования | ✓ | **Logback** через SLF4J (default для Spring Boot). Логи каждого сервиса (`Logger log = LoggerFactory.getLogger(...)`), уровень `INFO`, debug-режим для security. Логи rate-limiter, сидинга, email-дайджеста. | `application.yml`, все `*Service.java` |
| 4 | RESTful API | ✓ | 16 эндпоинтов под `/api/v1/**`: проекты, задачи, комментарии, админка. HTTP Basic auth, JSON, DTO. **OpenAPI 3.0 + Swagger UI** на `/swagger-ui.html` (springdoc 2.8.6). | `controller/api/`, `dto/api/` |
| 5 | Разграничение доступа по ролям (ADMIN / USER) | ✓ | Две роли: `ADMIN` (видит всё, доступ к `/admin/**` и `/monitoring/**`) и `USER` (только свои проекты). Защита на URL-уровне (`SecurityConfig`) **и продублирована** через `@PreAuthorize` на бизнес-методах. | `SecurityConfig.java`, `*Service.java` с `@PreAuthorize`, `entity/Role.java` |
| 6 | Многопоточность и асинхронность для долгих операций | ✓ | **`@EnableAsync`** + кастомный `ThreadPoolTaskExecutor` (core=2, max=8, queue=50). **`@EnableScheduling`**: 2 фоновых задачи: обновление кеша статистики (`StatsService` + `@Scheduled(fixedDelay=…)`) и **email-дайджест** (`EmailNotificationService` + `@Scheduled(cron="0 0 8 * * *")`). | `AsyncConfig.java`, `StatsService.java`, `EmailNotificationService.java` |
| 7 | Подключена JavaMelody | ✓ | **JavaMelody 2.5.0** через `javamelody-spring-boot-starter`. UI `/monitoring` доступен только `ADMIN`. Меряет JVM, SQL, HTTP, кэши. | `pom.xml`, `SecurityConfig.java` |
| 8 | Приложение в Docker-образе | ✓ | **Multi-stage Dockerfile** на distroless. **docker-compose.yml** с Postgres. Тестируется в Testcontainers IT-тестами. | `Dockerfile`, `docker-compose.yml` |
| 9 | Написана документация | ✓ | Этот файл, `README.md`, `WHATS-DONE.md`, `HOW-IT-WORKS.md`, `DEPLOYMENT.md`, Javadoc, OpenAPI/Swagger. | `*.md` в корне, `docs/` |

## IV. Что может включать в себя ТЗ (из методички)

| Раздел | Где раскрыт |
| --- | --- |
| ➔ Название проекта | `README.md` (заголовок) |
| ➔ Описание проекта | `README.md` (intro), `WHATS-DONE.md` § 1 |
| ➔ Сущности, примеры сущностей | `WHATS-DONE.md` § 1 (таблица), `entity/` |
| ➔ Функциональные требования (USER / ADMIN) | `README.md` (раздел «Доступ») |
| ➔ Безопасность | `WHATS-DONE.md` § 2, `HOW-IT-WORKS.md` § 7 |
| ➔ Интеграции и инструменты | `pom.xml`, `README.md` (стек) |
| ➔ Базы данных | `WHATS-DONE.md` § 1 (миграции), `db/migration/` |
| ➔ Тестирование | `WHATS-DONE.md` § 11 |
| ➔ Архитектура | `HOW-IT-WORKS.md` § 1-5 |
| ➔ Стек технологий | `README.md` (таблица) |
| ➔ Документация (планируемая) | этот файл + WHATS-DONE + HOW-IT-WORKS + DEPLOYMENT + Swagger |
| ➔ Дополнительные опции | См. v1-v4 quick-wins (`WHATS-DONE.md` § 5-8) |

## V. ТЗ проекта (специфика ProjectHub)

Из `docs/TZ-projecthub.pdf`:

| Бизнес-функция | Статус | Где |
| --- | --- | --- |
| Регистрация и логин пользователя | ✓ | `/register`, `/login` |
| Создание проекта (название, описание, статус, эмодзи) | ✓ | `/projects/new` |
| Редактирование, удаление, просмотр проекта | ✓ | `/projects/{id}/edit`, `/projects/{id}` |
| Создание задачи внутри проекта (название, описание, дедлайн, статус, приоритет, теги, исполнитель) | ✓ | `/projects/{id}/tasks/new` |
| Смена статуса задачи (TODO / IN_PROGRESS / DONE / BLOCKED) | ✓ | UI + drag-drop на канбане + `PATCH /api/v1/tasks/{id}/status` |
| Комментарии к задаче (с @-меншенами, Markdown недоступен, но HTML-escape есть) | ✓ | `/tasks/{id}` |
| Вложения к задаче (drag-drop, скачивание) | ✓ | `/tasks/{id}` |
| История изменений задачи (Envers ревизии) | ✓ | `/tasks/{id}/history` |
| Назначение исполнителей | ✓ | dropdown в форме задачи |
| Поиск по проектам и задачам | ✓ | глобальный навбар поиск + фильтры на странице проекта |
| Сортировка / пагинация задач | ✓ | `/projects/{id}?sort=priority,desc&page=2` |
| Дашборд «Мой день» | ✓ | `/dashboard` |
| Календарь дедлайнов проекта | ✓ | `/projects/{id}/calendar` |
| Burndown-график | ✓ | на странице проекта |
| CSV-экспорт задач | ✓ | `/projects/{id}/tasks/export.csv` |
| Избранные проекты | ✓ | звёздочка `POST /projects/{id}/star` |
| Email-уведомления о просроченных задачах | ✓ | `/settings`, `@Scheduled` |
| Админка: список пользователей, смена ролей | ✓ | `/admin/users` |
| Админка: сводная статистика | ✓ | `/admin/stats` |
| Мониторинг приложения | ✓ | `/monitoring` (только ADMIN) |

## VI. Видео-демо (требование ТЗ-Naumen § 4)

Сценарий 2-3 минут готов в `docs/projecthub-video-script.md`:

| Время | Сцена | Требование ТЗ |
| --- | --- | --- |
| 0:00–0:10 | Логин (`admin/admin123`) | авторизация + GUI |
| 0:10–0:30 | Дашборд: счётчики, графики, ачивки | GUI, таблицы |
| 0:30–0:45 | Глобальный поиск + хоткеи (`?`) | UX, навигация |
| 0:45–1:10 | Список проектов: ⭐, прогресс-бары, эмодзи | HTMX, паттерны |
| 1:10–1:30 | Создание проекта с Markdown + toast | формы ввода, безопасность (XSS) |
| 1:30–2:00 | Канбан drag-drop + конфетти + история (Envers) | ORM Hibernate, асинхронность |
| 2:00–2:15 | Календарь дедлайнов + тёмная тема | GUI |
| 2:15–2:35 | Swagger UI + админка | REST API, RBAC |
| 2:35–2:50 | H2-console: `SELECT login, password` — BCrypt-хеши | шифрование паролей |
| 2:50–3:00 | JavaMelody `/monitoring` + финал «35 unit + 50 IT» | мониторинг, тесты |

## VII. Итоговое покрытие

| Категория | Покрыто | Всего | Доля |
| --- | --- | --- | --- |
| Обязательные технические требования | 7 | 7 | **100%** |
| Опциональные требования | 9 | 9 | **100%** |
| Общие организационные требования | 6 | 6 | **100%** |
| Бизнес-функции из ТЗ проекта | 20 | 20 | **100%** |

Все пункты ТЗ закрыты. Дополнительно реализовано 20+ quick-wins (v1-v4), которые
ТЗ напрямую не требует, но которые делают приложение визуально и
функционально убедительнее — полный список в `WHATS-DONE.md`.
