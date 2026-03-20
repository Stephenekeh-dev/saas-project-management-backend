CREATE TABLE projects (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL REFERENCES tenants(id),
    created_by  BIGINT REFERENCES users(id),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    status      VARCHAR(50),
    start_date  DATE,
    end_date    DATE,
    created_at  TIMESTAMP
);