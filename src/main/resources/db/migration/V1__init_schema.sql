CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL UNIQUE,
    customer_id VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(64) NOT NULL,
    card_token VARCHAR(128) NOT NULL,
    amount NUMERIC(15,2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    event_time TIMESTAMP NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    device_id VARCHAR(128),
    country VARCHAR(2),
    city VARCHAR(128),
    payment_method VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE fraud_decisions (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL UNIQUE,
    risk_score INTEGER NOT NULL CHECK (risk_score >= 0),
    decision VARCHAR(16) NOT NULL CHECK (decision IN ('ALLOW', 'REVIEW', 'DECLINE')),
    decision_reason TEXT,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_fraud_decisions_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
);

CREATE TABLE rule_triggers (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(128) NOT NULL,
    score_added INTEGER NOT NULL CHECK (score_added >= 0),
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_rule_triggers_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
);

CREATE TABLE rule_configs (
    id BIGSERIAL PRIMARY KEY,
    rule_code VARCHAR(64) NOT NULL UNIQUE,
    rule_name VARCHAR(128) NOT NULL,
    description TEXT,
    weight INTEGER NOT NULL CHECK (weight >= 0),
    threshold_value NUMERIC(15,2),
    time_window_sec INTEGER,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE customer_profiles (
    id BIGSERIAL PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL UNIQUE,
    avg_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    tx_count_24h INTEGER NOT NULL DEFAULT 0,
    last_country VARCHAR(2),
    last_city VARCHAR(128),
    last_device_id VARCHAR(128),
    last_transaction_time TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO rule_configs (rule_code, rule_name, description, weight, threshold_value, time_window_sec, is_enabled, updated_at)
VALUES
    ('HIGH_AMOUNT', 'Высокая сумма', 'Срабатывает при сумме выше заданного порога', 25, 50000, NULL, TRUE, NOW()),
    ('VERY_HIGH_AMOUNT', 'Очень высокая сумма', 'Срабатывает при очень большой сумме', 45, 100000, NULL, TRUE, NOW()),
    ('NEW_DEVICE', 'Новое устройство', 'Срабатывает при смене устройства клиента', 15, NULL, NULL, TRUE, NOW()),
    ('NEW_COUNTRY', 'Новая страна', 'Срабатывает при смене страны', 20, NULL, NULL, TRUE, NOW()),
    ('NIGHT_TRANSACTION', 'Ночная транзакция', 'Срабатывает для операций в ночные часы', 10, NULL, NULL, TRUE, NOW()),
    ('VELOCITY_24H', 'Скользящее окно операций', 'Срабатывает при аномальной частоте операций в заданном окне', 20, 4, 3600, TRUE, NOW()),
    ('HIGH_Z_SCORE', 'Аномальная сумма по Z-score', 'Срабатывает при сильном отклонении суммы от истории клиента', 25, 2.50, NULL, TRUE, NOW()),
    ('HIGH_RISK_COEFFICIENT', 'Высокий агрегированный риск', 'Срабатывает по совокупному коэффициенту риска', 20, 0.65, NULL, TRUE, NOW()),
    ('HIGH_UNUSUALNESS', 'Необычная транзакция', 'Срабатывает при высоком индексе необычности', 15, 60, NULL, TRUE, NOW());
