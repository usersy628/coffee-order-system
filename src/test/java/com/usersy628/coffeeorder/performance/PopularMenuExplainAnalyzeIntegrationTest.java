package com.usersy628.coffeeorder.performance;

import com.usersy628.coffeeorder.popularity.application.PopularMenu;
import com.usersy628.coffeeorder.popularity.application.PopularMenuQueryRepository;
import com.usersy628.coffeeorder.support.testcontainers.MySqlIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("performance")
@MySqlIntegrationTest
@Transactional
@TestPropertySource(properties = "spring.datasource.hikari.data-source-properties.rewriteBatchedStatements=true")
class PopularMenuExplainAnalyzeIntegrationTest {

    private static final int ORDER_COUNT = 100_000;
    private static final int ITEMS_PER_ORDER = 3;
    private static final int ITEM_COUNT = ORDER_COUNT * ITEMS_PER_ORDER;
    private static final int RECENT_ORDER_COUNT = 70_000;
    private static final int PRECEDING_ORDER_COUNT = ORDER_COUNT - RECENT_ORDER_COUNT;
    private static final int MENU_COUNT = 100;
    private static final int BATCH_SIZE = 1_000;
    private static final long MENU_ID_BASE = 10_000L;
    private static final long ORDER_ID_BASE = 1_000_000L;
    private static final long ORDER_ITEM_ID_BASE = 2_000_000L;
    private static final Instant WINDOW_TO = Instant.parse("2026-07-15T00:00:00Z");
    private static final Instant WINDOW_FROM = WINDOW_TO.minus(168, ChronoUnit.HOURS);
    private static final long RECENT_WINDOW_SECONDS = ChronoUnit.SECONDS.between(WINDOW_FROM, WINDOW_TO);
    private static final long PRECEDING_WINDOW_SECONDS = ChronoUnit.DAYS.getDuration().multipliedBy(23).getSeconds();

    private static final String INSERT_ORDER = """
        INSERT INTO orders (id, user_id, idempotency_key, request_hash, total_amount, status, created_at, paid_at)
        VALUES (?, 1, ?, ?, 6000, 'PAID', ?, ?)
        """;

    private static final String INSERT_ITEM = """
        INSERT INTO order_item (id, order_id, menu_id, menu_name, unit_price, quantity, line_amount)
        VALUES (?, ?, ?, ?, 1000, ?, ?)
        """;

    private static final String EXPLAIN_POPULAR_MENU_QUERY = """
        EXPLAIN ANALYZE
        SELECT oi.menu_id,
               m.name AS menu_name,
               SUM(oi.quantity) AS total_quantity
        FROM orders o
        JOIN order_item oi ON oi.order_id = o.id
        JOIN menu m ON m.id = oi.menu_id
        WHERE o.paid_at >= ?
          AND o.paid_at < ?
        GROUP BY oi.menu_id, m.name
        ORDER BY total_quantity DESC, oi.menu_id ASC
        LIMIT 3
        """;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PopularMenuQueryRepository popularMenuQueryRepository;

    @BeforeEach
    void setUp() {
        deletePerformanceFixture();
        insertPerformanceMenus();
        insertOrders();
        insertItems();
    }

    @Test
    void recordsPopularMenuExplainAnalyzeForThirtyDayFixture() throws IOException {
        assertThat(count("orders", ORDER_ID_BASE, ORDER_ID_BASE + ORDER_COUNT)).isEqualTo(ORDER_COUNT);
        assertThat(count("order_item", ORDER_ITEM_ID_BASE, ORDER_ITEM_ID_BASE + ITEM_COUNT)).isEqualTo(ITEM_COUNT);
        assertThat(countPaidAtBetween(WINDOW_FROM, WINDOW_TO)).isEqualTo(RECENT_ORDER_COUNT);
        assertThat(countPaidAtBetween(WINDOW_FROM.minus(23, ChronoUnit.DAYS), WINDOW_FROM)).isEqualTo(PRECEDING_ORDER_COUNT);
        assertThat(indexColumns("orders", "idx_orders_paid_at")).containsExactly("paid_at");
        assertThat(indexColumns("order_item", "uk_order_item_order_menu")).containsExactly("order_id", "menu_id");

        List<PopularMenu> popularMenus = popularMenuQueryRepository.findTopThreeByPaidAtBetween(WINDOW_FROM, WINDOW_TO);
        assertThat(popularMenus).hasSize(3);
        assertThat(popularMenus).allSatisfy(menu -> assertThat(menu.totalQuantity()).isPositive());

        String plan = String.join(System.lineSeparator(), jdbcTemplate.query(
            EXPLAIN_POPULAR_MENU_QUERY,
            (resultSet, rowNum) -> resultSet.getString(1),
            Timestamp.from(WINDOW_FROM),
            Timestamp.from(WINDOW_TO)
        ));

        assertThat(plan).isNotBlank();

        Path report = Path.of("build", "reports", "performance", "popular-menu-explain-analyze.txt");
        Files.createDirectories(report.getParent());
        Files.writeString(report, reportBody(plan), StandardCharsets.UTF_8);

        assertThat(Files.exists(report)).isTrue();
    }

    private void insertPerformanceMenus() {
        jdbcTemplate.batchUpdate("""
            INSERT INTO menu (id, name, price, status, created_at, updated_at)
            VALUES (?, ?, 1000, 'ON_SALE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
            ON DUPLICATE KEY UPDATE name = VALUES(name), price = VALUES(price), status = VALUES(status),
                                    updated_at = VALUES(updated_at)
            """, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement statement, int index) throws SQLException {
                statement.setLong(1, menuId(index));
                statement.setString(2, menuName(index));
            }

            @Override
            public int getBatchSize() {
                return MENU_COUNT;
            }
        });
    }

    private void insertOrders() {
        for (int batchStart = 0; batchStart < ORDER_COUNT; batchStart += BATCH_SIZE) {
            int currentBatchStart = batchStart;
            int currentBatchSize = Math.min(BATCH_SIZE, ORDER_COUNT - currentBatchStart);
            jdbcTemplate.batchUpdate(INSERT_ORDER, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement statement, int rowIndex) throws SQLException {
                    int orderIndex = currentBatchStart + rowIndex;
                    Timestamp paidAt = Timestamp.from(paidAt(orderIndex));
                    statement.setLong(1, ORDER_ID_BASE + orderIndex);
                    statement.setString(2, "s11-performance-order-" + orderIndex);
                    statement.setString(3, "p".repeat(64));
                    statement.setTimestamp(4, paidAt);
                    statement.setTimestamp(5, paidAt);
                }

                @Override
                public int getBatchSize() {
                    return currentBatchSize;
                }
            });
        }
    }

    private void insertItems() {
        for (int batchStart = 0; batchStart < ITEM_COUNT; batchStart += BATCH_SIZE) {
            int currentBatchStart = batchStart;
            int currentBatchSize = Math.min(BATCH_SIZE, ITEM_COUNT - currentBatchStart);
            jdbcTemplate.batchUpdate(INSERT_ITEM, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement statement, int rowIndex) throws SQLException {
                    int itemIndex = currentBatchStart + rowIndex;
                    int orderIndex = itemIndex / ITEMS_PER_ORDER;
                    int itemOffset = itemIndex % ITEMS_PER_ORDER;
                    int quantity = itemOffset + 1;
                    int menuIndex = (orderIndex + itemOffset * 37) % MENU_COUNT;
                    statement.setLong(1, ORDER_ITEM_ID_BASE + itemIndex);
                    statement.setLong(2, ORDER_ID_BASE + orderIndex);
                    statement.setLong(3, menuId(menuIndex));
                    statement.setString(4, menuName(menuIndex));
                    statement.setInt(5, quantity);
                    statement.setLong(6, quantity * 1000L);
                }

                @Override
                public int getBatchSize() {
                    return currentBatchSize;
                }
            });
        }
    }

    private void deletePerformanceFixture() {
        long orderIdEndExclusive = ORDER_ID_BASE + ORDER_COUNT;
        jdbcTemplate.update("DELETE FROM order_event_outbox WHERE order_id >= ? AND order_id < ?", ORDER_ID_BASE, orderIdEndExclusive);
        jdbcTemplate.update("DELETE FROM point_history WHERE order_id >= ? AND order_id < ?", ORDER_ID_BASE, orderIdEndExclusive);
        jdbcTemplate.update("DELETE FROM order_item WHERE id >= ? AND id < ?", ORDER_ITEM_ID_BASE, ORDER_ITEM_ID_BASE + ITEM_COUNT);
        jdbcTemplate.update("DELETE FROM orders WHERE id >= ? AND id < ?", ORDER_ID_BASE, orderIdEndExclusive);
        jdbcTemplate.update("DELETE FROM menu WHERE id >= ? AND id < ?", MENU_ID_BASE, MENU_ID_BASE + MENU_COUNT);
    }

    private int count(String table, long startInclusive, long endExclusive) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + table + " WHERE id >= ? AND id < ?", Integer.class, startInclusive, endExclusive
        );
        return count == null ? 0 : count;
    }

    private int countPaidAtBetween(Instant fromInclusive, Instant toExclusive) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM orders
            WHERE id >= ?
              AND id < ?
              AND paid_at >= ?
              AND paid_at < ?
            """, Integer.class, ORDER_ID_BASE, ORDER_ID_BASE + ORDER_COUNT,
            Timestamp.from(fromInclusive), Timestamp.from(toExclusive));
        return count == null ? 0 : count;
    }

    private List<String> indexColumns(String table, String indexName) {
        return jdbcTemplate.queryForList("""
            SELECT column_name
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = ?
              AND index_name = ?
            ORDER BY seq_in_index
            """, String.class, table, indexName);
    }

    private Instant paidAt(int orderIndex) {
        if (orderIndex < RECENT_ORDER_COUNT) {
            long offsetSeconds = 1L + (long) orderIndex * (RECENT_WINDOW_SECONDS - 1) / (RECENT_ORDER_COUNT - 1);
            return WINDOW_TO.minusSeconds(offsetSeconds);
        }
        int precedingOrderIndex = orderIndex - RECENT_ORDER_COUNT;
        long offsetSeconds = 1L + (long) precedingOrderIndex * (PRECEDING_WINDOW_SECONDS - 1) / (PRECEDING_ORDER_COUNT - 1);
        return WINDOW_FROM.minusSeconds(offsetSeconds);
    }

    private long menuId(int menuIndex) {
        return MENU_ID_BASE + menuIndex;
    }

    private String menuName(int menuIndex) {
        return "S11 performance menu-" + String.format("%03d", menuIndex);
    }

    private String reportBody(String plan) {
        return """
            # S11 popular menu EXPLAIN ANALYZE
            # orders: 100000
            # order_item: 300000
            # menu: 100
            # paid_at distribution: 70000 orders inside the final 168-hour window, 30000 orders in the preceding 23 days
            # query window: [%s, %s)
            # This output is an environment-specific observation, not a timing assertion.

            %s
            """.formatted(WINDOW_FROM, WINDOW_TO, plan);
    }
}
