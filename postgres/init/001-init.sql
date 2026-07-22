CREATE SCHEMA IF NOT EXISTS lab;

CREATE TABLE IF NOT EXISTS lab.orders (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id  VARCHAR(64) NOT NULL,
    status       VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    total_amount NUMERIC(12, 2) NOT NULL CHECK (total_amount >= 0),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO lab.orders (customer_id, status, total_amount)
VALUES
    ('customer-001', 'CREATED', 49.90),
    ('customer-002', 'PAID', 129.00);
