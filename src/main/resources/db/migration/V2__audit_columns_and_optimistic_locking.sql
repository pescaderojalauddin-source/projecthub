-- ProjectHub V2: JPA Auditing (createdBy/updatedAt/updatedBy) + @Version (оптимистичная блокировка).
-- Совместимо с PostgreSQL 16 и с H2 в режиме PostgreSQL.
--
-- Новые колонки добавляются с DEFAULT'ами для существующих строк:
--   * version    BIGINT NOT NULL DEFAULT 0     — счётчик ревизии для @Version
--   * updated_at TIMESTAMP NULL                — заполняется при последующих UPDATE'ах
--   * created_by VARCHAR(64) NULL              — логин автора (или 'system' для seed-данных)
--   * updated_by VARCHAR(64) NULL              — логин последнего редактора
--
-- users получает только version + updated_at (обоюдная аудит-связь "пользователь правит пользователя"
-- избыточна, для этого есть Spring Data Envers / отдельный audit-лог).

-- USERS -----------------------------------------------------------------------
ALTER TABLE users ADD COLUMN version    BIGINT    NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN updated_at TIMESTAMP NULL;
UPDATE users SET updated_at = created_at WHERE updated_at IS NULL;

-- PROJECTS --------------------------------------------------------------------
ALTER TABLE projects ADD COLUMN version    BIGINT      NOT NULL DEFAULT 0;
ALTER TABLE projects ADD COLUMN updated_at TIMESTAMP   NULL;
ALTER TABLE projects ADD COLUMN created_by VARCHAR(64) NULL;
ALTER TABLE projects ADD COLUMN updated_by VARCHAR(64) NULL;
UPDATE projects SET updated_at = created_at WHERE updated_at IS NULL;
UPDATE projects p
SET created_by = (SELECT u.login FROM users u WHERE u.id = p.owner_id)
WHERE p.created_by IS NULL;

-- TASKS -----------------------------------------------------------------------
ALTER TABLE tasks ADD COLUMN version    BIGINT      NOT NULL DEFAULT 0;
ALTER TABLE tasks ADD COLUMN updated_at TIMESTAMP   NULL;
ALTER TABLE tasks ADD COLUMN created_by VARCHAR(64) NULL;
ALTER TABLE tasks ADD COLUMN updated_by VARCHAR(64) NULL;
UPDATE tasks SET updated_at = created_at WHERE updated_at IS NULL;

-- COMMENTS --------------------------------------------------------------------
ALTER TABLE comments ADD COLUMN version    BIGINT      NOT NULL DEFAULT 0;
ALTER TABLE comments ADD COLUMN updated_at TIMESTAMP   NULL;
ALTER TABLE comments ADD COLUMN created_by VARCHAR(64) NULL;
ALTER TABLE comments ADD COLUMN updated_by VARCHAR(64) NULL;
UPDATE comments SET updated_at = created_at WHERE updated_at IS NULL;
UPDATE comments c
SET created_by = (SELECT u.login FROM users u WHERE u.id = c.author_id)
WHERE c.created_by IS NULL;
