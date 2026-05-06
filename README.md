# ProjectHub

[![CI](https://github.com/pescaderojalauddin-source/projecthub/actions/workflows/ci.yml/badge.svg)](https://github.com/pescaderojalauddin-source/projecthub/actions/workflows/ci.yml)

> Курсовой проект — система управления проектами и задачами на Spring Boot 3 + Thymeleaf + Bootstrap 5.

ProjectHub — веб-приложение с GUI для создания проектов, управления задачами,
назначения исполнителей, контроля сроков и ведения истории изменений через комментарии.
Реализованы:

- три слоя: `@Controller` + Thymeleaf-шаблоны, `@Service` (бизнес-логика и транзакции), `@Repository` (Spring Data JPA);
- сущности **User**, **Project**, **Task**, **Comment** со связями 1:N;
- роли **USER** / **ADMIN** (RBAC), `@PreAuthorize` + URL-правила;
- Spring Security: BCrypt-хэш паролей, форменный логин, **включённый CSRF**;
- валидация форм через `@Valid` (Jakarta Bean Validation);
- глобальный `@ControllerAdvice` и страницы 400/403/404/500 (HTML), отдельный `@RestControllerAdvice` для JSON-ошибок REST API;
- сводная статистика для администратора, поиск, пагинация, фильтрация задач;
- **REST API** под `/api/v1/**` (HTTP Basic, JSON, OpenAPI/Swagger);
- **JavaMelody** мониторинг на `/monitoring` (только ADMIN);
- **Многопоточность** (`@EnableAsync`, `@Scheduled`) — фоновое обновление кеша статистики;
- Unit-тесты (JUnit 5 + Mockito) для сервисов, интеграционные тесты HTTP/REST-слоя, **Testcontainers + Postgres** для проверки на реальной БД.

## Стек

| Слой | Технологии |
| --- | --- |
| Backend | Java 17, Spring Boot 3.5, Spring Web, Spring Security, Spring Data JPA, Validation |
| ORM | Hibernate / Jakarta Persistence |
| Миграции | Flyway (`src/main/resources/db/migration`) |
| GUI | Thymeleaf, Bootstrap 5 (webjars), Bootstrap Icons |
| БД | H2 (dev — по умолчанию) или PostgreSQL 16 (профиль `postgres`) |
| Сборка | Maven (через `./mvnw` wrapper) |
| Документация API | springdoc-openapi 2.x → `/swagger-ui.html` |
| Тесты | JUnit 5, Mockito, Spring Boot Test, MockMvc, H2, Testcontainers (Postgres) |
| Мониторинг | Spring Boot Actuator (`/actuator/health`), JavaMelody (`/monitoring`, ADMIN) |

## Быстрый старт (без установки чего-либо лишнего)

```bash
git clone https://github.com/pescaderojalauddin-source/projecthub.git
cd projecthub
./mvnw spring-boot:run
```

Откройте `http://localhost:8080` — логин-форма. Демо-учётки:

| Логин | Пароль | Роль |
| --- | --- | --- |
| `admin` | `admin123` | `ADMIN` |
| `ivan`  | `user123`  | `USER` |
| `maria` | `user123`  | `USER` |

В dev-профиле H2 console доступна на `http://localhost:8080/h2-console`
(JDBC URL: `jdbc:h2:mem:projecthub`, user: `sa`, без пароля).

Swagger UI: `http://localhost:8080/swagger-ui.html`.

## Запуск с PostgreSQL

```bash
docker compose up -d        # поднимает Postgres 16 на :5432
SPRING_PROFILES_ACTIVE=postgres ./mvnw spring-boot:run
```

Учётные данные БД (можно переопределить через переменные окружения):

| Переменная | По умолчанию |
| --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/projecthub` |
| `SPRING_DATASOURCE_USERNAME` | `projecthub` |
| `SPRING_DATASOURCE_PASSWORD` | `projecthub` |

## Сборка артефакта и Docker-образа

```bash
./mvnw -B clean package           # → target/projecthub-0.1.0.jar
docker build -t projecthub:0.1.0 .
```

## Тесты

```bash
./mvnw -B test            # юнит-тесты сервисов (Surefire, *Test*.java)
./mvnw -B verify          # запускает также интеграционные тесты *IT.java (Failsafe), JaCoCo-отчёт
TESTCONTAINERS=1 ./mvnw -B verify   # дополнительно поднимает Postgres-контейнер (PostgresContainerIT)
```

- Юнит-тесты сервисов: `*Test.java` в `src/test/java/.../service/`.
- Интеграционные HTTP/REST тесты: `AuthFlowIT`, `RestApiIT` (MockMvc + H2).
- Testcontainers: `PostgresContainerIT` — запускает реальный `postgres:16-alpine`, прогоняет Flyway-миграции и проверяет CRUD. Включается переменной окружения `TESTCONTAINERS=1` (в CI выставлено по умолчанию).

Конфиг тестов в `src/test/resources/application-test.yml` — H2 in-memory, сидинг отключён.

## JavaDoc

```bash
./mvnw -B javadoc:javadoc          # → target/site/apidocs/index.html
```

## Архитектура

```
com.example.projecthub
├── config/         # SecurityConfig, OpenApiConfig
├── controller/     # @Controller — обработка HTTP-запросов и Thymeleaf-рендеринг
├── service/        # @Service — бизнес-логика, @Transactional, RBAC проверки
├── repository/     # @Repository / Spring Data JPA
├── entity/         # @Entity — User, Project, Task, Comment, перечисления
├── dto/            # формы (RegistrationForm, ProjectForm, TaskForm, CommentForm)
├── exception/      # @ControllerAdvice, кастомные исключения
└── init/           # CommandLineRunner для сидинга демо-данных
```

Связи сущностей:

```
User 1 ── ∞ Project        (User.id = Project.owner_id)
Project 1 ── ∞ Task         (Project.id = Task.project_id)
Task 1 ── ∞ Comment         (Task.id = Comment.task_id)
User 1 ── ∞ Comment         (User.id = Comment.author_id)
User 1 ── ∞ Task (assignee) (User.id = Task.assignee_id, nullable)
```

## Безопасность

| Мера | Реализация |
| --- | --- |
| Аутентификация | Spring Security `formLogin`, `UserDetailsService` через `UserService` |
| Хранение паролей | `BCryptPasswordEncoder` |
| RBAC | `requestMatchers("/admin/**").hasRole("ADMIN")` + `@PreAuthorize` на сервисах/контроллерах |
| Объектная авторизация | проверки в `ProjectService.ensureAccessible()`, `TaskService.ensureAccessible()` |
| CSRF | `CookieCsrfTokenRepository.withHttpOnlyFalse()` (включён) |
| XSS | Thymeleaf `th:text` экранирует HTML по умолчанию |
| Валидация | `@Valid` + `BindingResult` на контроллерах, ограничения в DTO |
| Обработка ошибок | `GlobalExceptionHandler` с `@ControllerAdvice`, страницы 400/403/404/500 |

## Маршруты

| Метод | URL | Описание | Доступ |
| --- | --- | --- | --- |
| GET | `/` | редирект на /login или /projects | публично |
| GET | `/login` | страница логина | публично |
| GET/POST | `/register` | регистрация | публично |
| GET | `/projects` | список проектов (свои/все) | USER, ADMIN |
| GET/POST | `/projects/new`, `/projects` | создание проекта | USER, ADMIN |
| GET | `/projects/{id}` | карточка проекта + задачи | владелец / ADMIN |
| GET/POST | `/projects/{id}/edit`, `/projects/{id}` | редактирование | владелец / ADMIN |
| POST | `/projects/{id}/delete` | удаление | владелец / ADMIN |
| GET/POST | `/projects/{id}/tasks/new`, `/projects/{id}/tasks` | создание задачи | владелец / ADMIN |
| GET | `/tasks/{id}` | карточка задачи + комментарии | владелец / assignee / ADMIN |
| POST | `/tasks/{id}/status` | смена статуса | владелец / assignee / ADMIN |
| POST | `/tasks/{id}/comments` | добавить комментарий | владелец / assignee / ADMIN |
| GET | `/admin/users` | список пользователей | ADMIN |
| POST | `/admin/users/{id}/role` | сменить роль | ADMIN |
| GET | `/admin/stats` | сводная статистика | ADMIN |
| GET | `/swagger-ui.html` | OpenAPI документация | публично |
| GET | `/monitoring` | JavaMelody (нагрузка, JDBC, GC) | ADMIN |
| `*` | `/api/v1/**` | REST API (Basic Auth, JSON) | по ролям |

## Структура GUI

- общий layout `templates/fragments/layout.html` (навбар, флеш-сообщения, футер, пагинация);
- хлебные крошки на каждой странице;
- формы Bootstrap 5 с inline-валидацией (`is-invalid`, `invalid-feedback`);
- таблицы списков с поиском и сортировкой;
- кастомные страницы ошибок.

## REST API

Все эндпоинты под `/api/v1/**` отдают/принимают JSON, аутентификация — HTTP Basic
(USER/ADMIN, RBAC проверяется на сервисном слое).

Ключевые маршруты:

```
GET    /api/v1/projects                       — список проектов (постранично)
POST   /api/v1/projects                       — создать проект (201 + Location)
GET    /api/v1/projects/{id}                  — карточка проекта
PUT    /api/v1/projects/{id}                  — обновить
DELETE /api/v1/projects/{id}                  — удалить (204)

GET    /api/v1/projects/{projectId}/tasks     — задачи проекта
POST   /api/v1/projects/{projectId}/tasks     — создать задачу
GET    /api/v1/tasks/{id}                     — карточка задачи
PUT    /api/v1/tasks/{id}                     — обновить
PATCH  /api/v1/tasks/{id}/status              — сменить статус
DELETE /api/v1/tasks/{id}                     — удалить

GET    /api/v1/tasks/{taskId}/comments        — комментарии задачи
POST   /api/v1/tasks/{taskId}/comments        — добавить комментарий
DELETE /api/v1/comments/{id}                  — удалить (автор/ADMIN)

GET    /api/v1/admin/users                    — список пользователей (ADMIN)
PUT    /api/v1/admin/users/{id}/role          — сменить роль (ADMIN)
GET    /api/v1/admin/stats                    — сводная статистика (ADMIN)
```

Пример вызова:

```bash
curl -u ivan:user123 http://localhost:8080/api/v1/projects | jq
curl -u admin:admin123 http://localhost:8080/api/v1/admin/stats | jq
```

Интерактивная документация — Swagger UI: `http://localhost:8080/swagger-ui.html`.

## Скриншоты

После запуска приложения сделайте скриншоты ключевых экранов и положите их в `docs/screenshots/`.
Эту секцию можно дополнить при сдаче.

## Документация / ТЗ

Копии ТЗ положены в `docs/`:

- [docs/TZ-projecthub.pdf](docs/TZ-projecthub.pdf) — техническое задание ProjectHub.
- [docs/TZ-naumen-java.pdf](docs/TZ-naumen-java.pdf) — общее ТЗ практики на Java (Naumen).

## Видео-демо

> Ссылка на видео будет добавлена сюда: `TODO: <YouTube/Yandex Disk/Google Drive URL>`

Сценарий 2–3 минуты:

1. Регистрация нового пользователя.
2. Логин под `ivan/user123`.
3. Создание проекта, добавление 2 задач, смена статусов, комментарий.
4. Логин под `admin/admin123`: список пользователей, смена роли, страница статистики.
5. Демонстрация ошибки 403 (попытка зайти на `/admin/users` под обычным пользователем).
6. (Опционально) Вызов нескольких эндпоинтов REST API через `curl`/Swagger.

## Деплой

См. [`DEPLOYMENT.md`](DEPLOYMENT.md) — пошаговая инструкция по деплою на Render
(Blueprint и ручной), Railway, Fly.io, VPS. В корне репо лежит готовый
`render.yaml` для 1-click развёртывания.

## Лицензия

MIT — для учебного проекта.
