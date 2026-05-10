-- Эмодзи-иконки проектов и таблица разблокированных ачивок.
ALTER TABLE projects ADD COLUMN emoji VARCHAR(8);

CREATE TABLE achievements_unlocked (
    user_id   BIGINT       NOT NULL,
    code      VARCHAR(64)  NOT NULL,
    unlocked_at TIMESTAMP  NOT NULL,
    CONSTRAINT pk_achievements_unlocked PRIMARY KEY (user_id, code),
    CONSTRAINT fk_achievements_unlocked_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_achievements_unlocked_user ON achievements_unlocked (user_id);
