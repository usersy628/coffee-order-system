CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE point_wallet (
    user_id BIGINT NOT NULL,
    balance BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_point_wallet PRIMARY KEY (user_id),
    CONSTRAINT ck_point_wallet_balance
        CHECK (balance BETWEEN 0 AND 300000),
    CONSTRAINT fk_point_wallet_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE menu (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    price BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_menu PRIMARY KEY (id),
    CONSTRAINT ck_menu_price CHECK (price >= 1),
    CONSTRAINT ck_menu_status CHECK (status IN ('ON_SALE', 'STOPPED'))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    idempotency_key VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
    request_hash CHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
    total_amount BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    paid_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT uk_orders_user_idempotency_key UNIQUE (user_id, idempotency_key),
    CONSTRAINT ck_orders_total_amount CHECK (total_amount >= 1),
    CONSTRAINT ck_orders_status CHECK (status = 'PAID'),
    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_orders_paid_at (paid_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE order_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    menu_name VARCHAR(100) NOT NULL,
    unit_price BIGINT NOT NULL,
    quantity INT NOT NULL,
    line_amount BIGINT NOT NULL,
    CONSTRAINT pk_order_item PRIMARY KEY (id),
    CONSTRAINT uk_order_item_order_menu UNIQUE (order_id, menu_id),
    CONSTRAINT ck_order_item_unit_price CHECK (unit_price >= 1),
    CONSTRAINT ck_order_item_quantity CHECK (quantity >= 1),
    CONSTRAINT ck_order_item_line_amount
        CHECK (line_amount = unit_price * quantity),
    CONSTRAINT fk_order_item_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT,
    CONSTRAINT fk_order_item_menu
        FOREIGN KEY (menu_id) REFERENCES menu (id) ON DELETE RESTRICT,
    INDEX idx_order_item_menu_id (menu_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE point_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    order_id BIGINT NULL,
    idempotency_key VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NULL,
    request_hash CHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_point_history PRIMARY KEY (id),
    CONSTRAINT uk_point_history_order UNIQUE (order_id),
    CONSTRAINT uk_point_history_user_idempotency_key UNIQUE (user_id, idempotency_key),
    CONSTRAINT ck_point_history_operation CHECK (
        (
            type = 'CHARGE'
            AND amount > 0
            AND order_id IS NULL
            AND idempotency_key IS NOT NULL
            AND request_hash IS NOT NULL
        )
        OR
        (
            type = 'USE'
            AND amount < 0
            AND order_id IS NOT NULL
            AND idempotency_key IS NULL
            AND request_hash IS NULL
        )
    ),
    CONSTRAINT ck_point_history_balance_after
        CHECK (balance_after BETWEEN 0 AND 300000),
    CONSTRAINT fk_point_history_wallet
        FOREIGN KEY (user_id) REFERENCES point_wallet (user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_point_history_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE order_event_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
    order_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    schema_version INT NOT NULL DEFAULT 1,
    payload JSON NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NULL,
    locked_at DATETIME(6) NULL,
    claim_token CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NULL,
    published_at DATETIME(6) NULL,
    failed_at DATETIME(6) NULL,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_order_event_outbox PRIMARY KEY (id),
    CONSTRAINT uk_order_event_outbox_event_id UNIQUE (event_id),
    CONSTRAINT uk_order_event_outbox_order_type UNIQUE (order_id, event_type),
    CONSTRAINT ck_order_event_outbox_event_type
        CHECK (event_type = 'ORDER_COMPLETED'),
    CONSTRAINT ck_order_event_outbox_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT ck_order_event_outbox_attempt_count
        CHECK (attempt_count BETWEEN 0 AND 6),
    CONSTRAINT ck_order_event_outbox_schema_version
        CHECK (schema_version >= 1),
    CONSTRAINT ck_order_event_outbox_state_fields CHECK (
        (
            status = 'PENDING'
            AND attempt_count < 6
            AND next_attempt_at IS NOT NULL
            AND locked_at IS NULL
            AND claim_token IS NULL
            AND published_at IS NULL
            AND failed_at IS NULL
        )
        OR
        (
            status = 'PROCESSING'
            AND attempt_count BETWEEN 1 AND 6
            AND next_attempt_at IS NULL
            AND locked_at IS NOT NULL
            AND claim_token IS NOT NULL
            AND published_at IS NULL
            AND failed_at IS NULL
        )
        OR
        (
            status = 'PUBLISHED'
            AND attempt_count BETWEEN 1 AND 6
            AND next_attempt_at IS NULL
            AND locked_at IS NULL
            AND claim_token IS NULL
            AND published_at IS NOT NULL
            AND failed_at IS NULL
            AND last_error IS NULL
        )
        OR
        (
            status = 'FAILED'
            AND attempt_count BETWEEN 1 AND 6
            AND next_attempt_at IS NULL
            AND locked_at IS NULL
            AND claim_token IS NULL
            AND published_at IS NULL
            AND failed_at IS NOT NULL
            AND last_error IS NOT NULL
        )
    ),
    CONSTRAINT fk_order_event_outbox_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT,
    INDEX idx_order_event_outbox_status_next_attempt (status, next_attempt_at),
    INDEX idx_order_event_outbox_status_locked_at (status, locked_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
