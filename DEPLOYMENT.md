# Деплой ProjectHub

Это пошаговое руководство по публикации приложения в интернет. ProjectHub — это
полноценное Spring Boot 3 приложение с Thymeleaf-вьюхами и PostgreSQL, поэтому
ему нужен **PaaS / VPS, который умеет хостить Java**. Vercel этого делать не умеет
(см. раздел [«Почему не Vercel»](#почему-не-vercel) ниже).

Рекомендованный вариант — **Render**. Альтернативы: **Railway**, **Fly.io**, **VPS**.

## Содержание

1. [Render — рекомендованный путь (1-click через Blueprint)](#render--blueprint-1-click)
2. [Render — ручная настройка через UI](#render--ручная-настройка-через-ui)
3. [Альтернатива: Railway](#альтернатива-railway)
4. [Альтернатива: Fly.io](#альтернатива-flyio)
5. [Альтернатива: VPS (Ubuntu + Docker)](#альтернатива-vps-ubuntu--docker)
6. [Почему не Vercel](#почему-не-vercel)
7. [Какие переменные окружения читает приложение](#переменные-окружения)
8. [Чек-лист после деплоя](#чек-лист-после-деплоя)

---

## Render — Blueprint (1-click)

В корне репозитория уже лежит `render.yaml` — это Blueprint Render'а. Он описывает
веб-сервис + managed Postgres, привязывает их друг к другу через переменные окружения.

### Шаги

1. **Зарегистрируйтесь на Render** через GitHub: <https://dashboard.render.com>.
2. Нажмите **New +** → **Blueprint**.
3. Выберите репозиторий `projecthub`.
4. Render прочитает `render.yaml`, покажет план: 1 web-сервис (Docker, free) + 1 Postgres (free).
5. Нажмите **Apply** — Render клонирует репо, соберёт Docker-образ из `Dockerfile`, поднимет
   Postgres 16, прокинет в приложение `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD`,
   запустит контейнер с профилем `postgres`.
6. Первый деплой обычно занимает 5–8 минут (Maven-сборка + Spring Boot startup).
7. Когда статус сервиса станет **Live** — открывайте URL вида `https://projecthub-xxxx.onrender.com`.

### Что произойдёт автоматически на старте

- Spring Boot применит **Flyway-миграции** (`V1__init_schema.sql`, ...).
- **Сидинг по умолчанию выключен** (`PROJECTHUB_SEED_ENABLED=false` в `render.yaml`),
  чтобы захардкоженный `admin/admin123` не уехал в публичный доступ. Чтобы создать
  первого админа (одноразово):
  1. В Render → Service → Environment поставить `PROJECTHUB_SEED_ENABLED=true` и
     задать `PROJECTHUB_SEED_ADMIN_PASSWORD=<сильный_пароль>`.
  2. Перезапустить сервис — в логах появится `Сидинг ADMIN id=… login=admin`.
  3. После успешного логина обратно выключить `PROJECTHUB_SEED_ENABLED=false` и
     удалить `PROJECTHUB_SEED_ADMIN_PASSWORD`.
- Демо-пользователи `ivan`/`maria` и проекты управляются отдельным флагом
  `PROJECTHUB_SEED_DEMO_DATA_ENABLED` — для прода обычно держим `false`.

### Где смотреть логи

- Render → ваш сервис → вкладка **Logs**: stdout/stderr Spring Boot в реальном времени.
- `health-check` Render бьёт в `/actuator/health` — если он 200, сервис считается живым.

### Free-план: что важно знать

- Web-сервис на Free-плане **засыпает после 15 минут простоя** и просыпается при первом
  запросе (cold start ~30 секунд). Для курсовой защиты этого хватает.
- Free Postgres expires через 30 дней — Render показывает дедлайн в дашборде. Перед
  истечением можно сделать backup и пересоздать.
- Если нужен always-on — Starter ($7/мес).

---

## Render — ручная настройка через UI

Если не хотите Blueprint и хотите всё руками (например, чтобы понять, что внутри):

### 1. Создать Postgres

1. Render → **New +** → **PostgreSQL**.
2. Name: `projecthub-db`, Database: `projecthub`, User: `projecthub`, Region: Frankfurt, Plan: Free.
3. Create database. Подождите, пока статус станет **Available**.
4. На странице БД скопируйте блок **Connections**: `Hostname`, `Port`, `Database`, `Username`, `Password` —
   они понадобятся на шаге 2.

### 2. Создать Web Service

1. Render → **New +** → **Web Service**.
2. Connect repository: ваш `projecthub`.
3. Параметры:
   - **Name**: `projecthub`
   - **Region**: тот же, что у БД (`Frankfurt`).
   - **Branch**: `main` (или нужная).
   - **Runtime**: **Docker**
   - **Dockerfile Path**: `./Dockerfile`
   - **Health Check Path**: `/actuator/health`
   - **Plan**: Free (или выше).
4. Раскройте **Advanced** → **Environment Variables** и добавьте:

| Key | Value |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `postgres` |
| `PROJECTHUB_SEED_ENABLED` | `false` (см. «Безопасный сидинг» в README) |
| `PROJECTHUB_SEED_ADMIN_PASSWORD` | задаётся вручную при первом старте |
| `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=75` |
| `DB_HOST` | значение `Hostname` из БД (внутренний URL без `.render.com`, как `dpg-xxxx-a`) |
| `DB_PORT` | `5432` |
| `DB_NAME` | `projecthub` |
| `DB_USER` | `projecthub` |
| `DB_PASSWORD` | значение `Password` из БД |

> Внутри одного региона Render web-сервис может ходить в БД по **внутреннему хосту**
> (`dpg-xxxx-a`) — это бесплатно и быстрее. Внешний хост (`xxxx.frankfurt-postgres.render.com`)
> платный по трафику и нужен только для подключения снаружи (например, из DBeaver).

5. **Create Web Service**. Render склонирует репо, соберёт Docker, поднимет контейнер.

---

## Альтернатива: Railway

Очень похоже на Render, но интерфейс более автомагический.

1. <https://railway.app> → **New Project** → **Deploy from GitHub repo** → выбрать `projecthub`.
2. Railway автоматически детектит Dockerfile и собирает.
3. **+ New** → **Database** → **PostgreSQL**. Railway сразу подкинет переменные `PGHOST`, `PGPORT`,
   `PGDATABASE`, `PGUSER`, `PGPASSWORD` в проект.
4. На сервисе `projecthub` → **Variables** → добавьте маппинг:

   ```
   SPRING_PROFILES_ACTIVE=postgres
   PROJECTHUB_SEED_ENABLED=false
   # для первого старта, чтобы создать админа:
   #   PROJECTHUB_SEED_ENABLED=true
   #   PROJECTHUB_SEED_ADMIN_PASSWORD=<сильный_пароль>
   DB_HOST=${{Postgres.PGHOST}}
   DB_PORT=${{Postgres.PGPORT}}
   DB_NAME=${{Postgres.PGDATABASE}}
   DB_USER=${{Postgres.PGUSER}}
   DB_PASSWORD=${{Postgres.PGPASSWORD}}
   ```

5. На сервисе `projecthub` → **Settings** → **Networking** → **Generate Domain** —
   получите публичный URL.
6. **Settings** → **Healthcheck Path**: `/actuator/health`.

Railway Free trial — $5 кредита, дальше ~$5/мес.

---

## Альтернатива: Fly.io

Free tier чуть скуднее, но globally distributed; платформа любит Docker, а Postgres
управляется командой `fly`.

```bash
# 1. Установить flyctl
curl -L https://fly.io/install.sh | sh

# 2. Логин
fly auth login

# 3. Создать приложение из текущей папки (использует Dockerfile)
fly launch --no-deploy --name projecthub --region fra
# Откажитесь, когда спросит про Postgres внутри launch — сделаем отдельно.

# 4. Поднять managed Postgres
fly postgres create --name projecthub-db --region fra --vm-size shared-cpu-1x --volume-size 1
fly postgres attach --app projecthub projecthub-db
# Это сетит DATABASE_URL в формате postgres://user:pass@host:5432/db

# 5. Достать host/port/user/pass из DATABASE_URL и проставить вручную:
fly secrets set --app projecthub \
  SPRING_PROFILES_ACTIVE=postgres \
  PROJECTHUB_SEED_ENABLED=false \
  # для первого старта добавьте на одну попытку:
  #   PROJECTHUB_SEED_ENABLED=true PROJECTHUB_SEED_ADMIN_PASSWORD='<сильный_пароль>'

  DB_HOST=projecthub-db.flycast \
  DB_PORT=5432 \
  DB_NAME=projecthub \
  DB_USER=postgres \
  DB_PASSWORD='<пароль_из_attach_вывода>'

# 6. Деплой
fly deploy
```

В сгенерированном `fly.toml` укажите health check:

```toml
[[services]]
  internal_port = 8080
  protocol = "tcp"

  [[services.http_checks]]
    interval = "30s"
    timeout = "5s"
    method = "get"
    path = "/actuator/health"
```

---

## Альтернатива: VPS (Ubuntu + Docker)

Для дешёвого VPS (Hetzner CX11 €4.5/мес, Aeza, Selectel и т.п.):

```bash
# на VPS:
sudo apt update && sudo apt install -y docker.io docker-compose-plugin git
git clone https://github.com/<ваш-аккаунт>/projecthub.git
cd projecthub

# поднять Postgres + приложение через расширенный compose:
cat > docker-compose.prod.yml <<'EOF'
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: projecthub
      POSTGRES_USER: projecthub
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - pg-data:/var/lib/postgresql/data
    restart: always

  app:
    build: .
    environment:
      SPRING_PROFILES_ACTIVE: postgres
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: projecthub
      DB_USER: projecthub
      DB_PASSWORD: ${POSTGRES_PASSWORD}
      PROJECTHUB_SEED_ENABLED: "false"
      # для bootstrap'а админа задайте на один запуск:
      # PROJECTHUB_SEED_ADMIN_PASSWORD: "<сильный_пароль>"
    ports:
      - "80:8080"
    depends_on:
      - postgres
    restart: always

volumes:
  pg-data:
EOF

POSTGRES_PASSWORD=$(openssl rand -hex 16) docker compose -f docker-compose.prod.yml up -d --build
```

Сверху можно поставить Caddy/Nginx для HTTPS — Caddy умеет автоматически брать
Let's Encrypt сертификат под ваш домен.

---

## Почему не Vercel

**Vercel не хостит Java/Spring Boot — вообще никак.**

Vercel — это платформа под:

- статические сайты (HTML/CSS/JS, Astro, Next.js static export);
- Node.js / Edge Functions / Python serverless functions с лимитом 10 секунд;
- frontend-фреймворки (Next.js, Nuxt, SvelteKit, Remix).

Под Java у Vercel **нет рантайма**. Запихнуть туда Spring Boot можно только косвенно:
выкатить отдельный JS-фронтенд на Vercel и через CORS/прокси ходить в API,
который живёт на Render/Railway/Fly. Для **этого** проекта нет смысла так делать,
потому что фронтенд — это Thymeleaf-шаблоны, рендерящиеся на сервере, и они
неотделимы от бэкенда.

Что **можно** на Vercel рядом с этим проектом, если очень хочется:

- сделать отдельный статический лендинг (`index.html` с описанием) и кнопкой «Открыть приложение»,
  ведущей на ваш Render-URL — но это уже не «деплой ProjectHub», а отдельная страница про него.

Если нужен второй провайдер «для красоты» — берите **Railway** или **Fly.io**, оба
полноценно хостят Java.

---

## Переменные окружения

| Переменная | Назначение | Примечание |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | какой Spring-профиль подключать | для прод — `postgres` |
| `PORT` | порт, на котором слушать | в Spring уже стоит `${PORT:8080}`. Render/Heroku/Railway проставят сами |
| `SPRING_DATASOURCE_URL` | прямой JDBC URL | можно использовать вместо `DB_*` |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | хост/порт/имя БД | используются, если `SPRING_DATASOURCE_URL` не задан |
| `DB_USER` / `DB_PASSWORD` | креды БД | то же самое, fallback для `SPRING_DATASOURCE_USERNAME/PASSWORD` |
| `PROJECTHUB_SEED_ENABLED` | мастер-выключатель сидинга | `false` по умолчанию; на разовый bootstrap поставьте `true` |
| `PROJECTHUB_SEED_ADMIN_PASSWORD` | пароль для первого админа | без него админ в проде НЕ создаётся |
| `PROJECTHUB_SEED_DEMO_DATA_ENABLED` | сидить ли demo-пользователей и проекты | `false` для прода |
| `JAVA_TOOL_OPTIONS` | флаги JVM | например `-XX:MaxRAMPercentage=75` для контейнеров с маленькой RAM |

---

## Чек-лист после деплоя

После того как сервис стал **Live**, пройдитесь руками:

- [ ] `https://<host>/actuator/health` → `{"status":"UP"}`
- [ ] `https://<host>/login` открывается, форма логина рендерится
- [ ] (после bootstrap-сидинга) логин под `admin/<ваш_пароль>` работает
- [ ] на странице `/projects` видны демо-проекты (значит сидинг отработал и БД подключена)
- [ ] `https://<host>/swagger-ui.html` открывается
- [ ] `https://<host>/api/v1/projects` через `curl -u ivan:user123` отдаёт JSON
- [ ] `https://<host>/monitoring` под `admin` открывает JavaMelody (под обычным юзером — 403)
- [ ] в логах нет `FlywayException` / `Connection refused`

Если что-то 502/503 — смотрите логи (Render → Logs / `fly logs` / `railway logs`).
Чаще всего это либо неверные `DB_*` переменные, либо Postgres ещё не успел подняться.
