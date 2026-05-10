-- ProjectHub V4: избранные проекты (звёздочки).
-- Many-to-many между users и projects. Один юзер может пометить любой проект,
-- который видит (свой или общий ADMIN-проект).

CREATE TABLE project_stars (
    user_id    BIGINT    NOT NULL,
    project_id BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_project_stars PRIMARY KEY (user_id, project_id),
    CONSTRAINT fk_project_stars_user    FOREIGN KEY (user_id)    REFERENCES users (id)    ON DELETE CASCADE,
    CONSTRAINT fk_project_stars_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE
);

CREATE INDEX idx_project_stars_user    ON project_stars (user_id);
CREATE INDEX idx_project_stars_project ON project_stars (project_id);
