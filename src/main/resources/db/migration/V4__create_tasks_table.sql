CREATE TABLE tasks (
    id           BIGSERIAL PRIMARY KEY,
    project_id   BIGINT NOT NULL REFERENCES projects(id),
    assigned_to  BIGINT REFERENCES users(id),
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    status       VARCHAR(50),
    priority     VARCHAR(50),
    due_date     DATE,
    created_at   TIMESTAMP
);