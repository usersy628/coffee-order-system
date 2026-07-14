CREATE TABLE mock_data_platform_received_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
    payload JSON NOT NULL,
    received_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_mock_data_platform_received_event PRIMARY KEY (id),
    CONSTRAINT uk_mock_data_platform_received_event_event_id UNIQUE (event_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
