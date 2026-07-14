package com.usersy628.coffeeorder.support.testcontainers;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MySqlIntegrationTest
class DatabaseSmokeTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void appliesBothMigrationsAndCreatesTheSevenDesignedTables() throws SQLException {
        Integer migrationCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE success = 1
                  AND version IN ('1', '2')
                """, Integer.class);

        Integer tableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN (
                      'users',
                      'point_wallet',
                      'point_history',
                      'menu',
                      'orders',
                      'order_item',
                      'order_event_outbox'
                  )
                """, Integer.class);

        assertThat(migrationCount).isEqualTo(2);
        assertThat(tableCount).isEqualTo(7);
        assertThatCode(flyway::validate).doesNotThrowAnyException();

        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:mysql:");
        }
    }

    @Test
    @Transactional
    void enforcesRepresentativeCheckForeignKeyAndUniqueConstraints() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE point_wallet SET balance = -1 WHERE user_id = 1"
        ))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_point_wallet_balance");

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO point_wallet (user_id, balance, updated_at)
                VALUES (999999, 0, UTC_TIMESTAMP(6))
                """))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("fk_point_wallet_user");

        String idempotencyKey = "database-smoke-unique-key";
        String requestHash = "a".repeat(64);
        jdbcTemplate.update("""
                INSERT INTO orders (
                    user_id, idempotency_key, request_hash, total_amount,
                    status, created_at, paid_at
                )
                VALUES (?, ?, ?, 4500, 'PAID', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
                """, 1L, idempotencyKey, requestHash);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO orders (
                    user_id, idempotency_key, request_hash, total_amount,
                    status, created_at, paid_at
                )
                VALUES (?, ?, ?, 4500, 'PAID', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
                """, 1L, idempotencyKey, requestHash))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("uk_orders_user_idempotency_key");

        Long orderId = jdbcTemplate.queryForObject("""
                SELECT id
                FROM orders
                WHERE user_id = ? AND idempotency_key = ?
                """, Long.class, 1L, idempotencyKey);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO order_item (
                    order_id, menu_id, menu_name, unit_price, quantity, line_amount
                )
                VALUES (?, 1, '아메리카노', 4500, 2, 1)
                """, orderId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_order_item_line_amount");

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO order_event_outbox (
                    event_id, order_id, event_type, schema_version, payload,
                    status, attempt_count, next_attempt_at, created_at
                )
                VALUES (UUID(), ?, 'ORDER_COMPLETED', 1, JSON_OBJECT(),
                        'PENDING', 0, NULL, UTC_TIMESTAMP(6))
                """, orderId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_order_event_outbox_state_fields");
    }

    @Test
    void seedsEveryUserWithExactlyOneZeroPointWallet() {
        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users",
                Integer.class
        );
        Integer walletCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM point_wallet",
                Integer.class
        );
        Integer invalidWalletCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM users u
                LEFT JOIN point_wallet pw ON pw.user_id = u.id
                WHERE pw.user_id IS NULL OR pw.balance <> 0
                """, Integer.class);

        assertThat(userCount).isPositive();
        assertThat(walletCount).isEqualTo(userCount);
        assertThat(invalidWalletCount).isZero();
    }

    @Test
    void configuresEachDatabaseSessionForUtcAndTwoSecondLockWait() {
        Integer lockWaitSeconds = jdbcTemplate.queryForObject(
                "SELECT @@session.innodb_lock_wait_timeout",
                Integer.class
        );
        String sessionTimeZone = jdbcTemplate.queryForObject(
                "SELECT @@session.time_zone",
                String.class
        );

        assertThat(lockWaitSeconds).isEqualTo(2);
        assertThat(sessionTimeZone).isEqualTo("+00:00");
    }

    @Test
    void exposesOnlyTheRequiredActuatorHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isNotFound());
    }
}
