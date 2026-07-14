package com.usersy628.coffeeorder.regression;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.usersy628.coffeeorder.global.error.DomainException;
import com.usersy628.coffeeorder.global.error.ErrorCode;
import com.usersy628.coffeeorder.mockplatform.application.MockDataPlatformService;
import com.usersy628.coffeeorder.order.application.OrderCommand;
import com.usersy628.coffeeorder.order.application.OrderResult;
import com.usersy628.coffeeorder.order.application.OrderService;
import com.usersy628.coffeeorder.outbox.application.DataPlatformClient;
import com.usersy628.coffeeorder.outbox.application.OutboxClaimTransactionExecutor;
import com.usersy628.coffeeorder.outbox.application.OutboxPublisher;
import com.usersy628.coffeeorder.outbox.application.OutboxPublisherProperties;
import com.usersy628.coffeeorder.outbox.application.OutboxRetryPolicy;
import com.usersy628.coffeeorder.outbox.application.OutboxStateTransactionExecutor;
import com.usersy628.coffeeorder.outbox.domain.DeliveryResult;
import com.usersy628.coffeeorder.point.application.PointChargeCommand;
import com.usersy628.coffeeorder.point.application.PointChargeService;
import com.usersy628.coffeeorder.popularity.application.PopularMenu;
import com.usersy628.coffeeorder.popularity.application.PopularMenuQueryService;
import com.usersy628.coffeeorder.support.testcontainers.MySqlIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@MySqlIntegrationTest
@Import(CrossFeatureMySqlRegressionIntegrationTest.MutableClockConfiguration.class)
class CrossFeatureMySqlRegressionIntegrationTest {

    private static final Instant ORDER_OCCURRED_AT = Instant.parse("2026-07-15T00:00:00.000001Z");
    private static final long USER_ID = 1L;
    private static final String ORDER_KEY = "s11-cross-feature-order";

    @Autowired
    private PointChargeService pointChargeService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PopularMenuQueryService popularMenuQueryService;

    @Autowired
    private MockDataPlatformService mockDataPlatformService;

    @Autowired
    private OutboxPublisherProperties outboxPublisherProperties;

    @Autowired
    private OutboxClaimTransactionExecutor claimTransactionExecutor;

    @Autowired
    private OutboxStateTransactionExecutor stateTransactionExecutor;

    @Autowired
    private OutboxRetryPolicy retryPolicy;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        ensureAssignmentMenus();
        clock.set(ORDER_OCCURRED_AT);
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void concurrentIdempotentOrderIsCommittedOnceRankedOnceAndDeliveredOnce() throws Exception {
        pointChargeService.charge(new PointChargeCommand(USER_ID, 30_000L, "s11-charge"));

        List<OrderResult> results = createSameOrderConcurrently();

        assertThat(results).hasSize(10);
        assertThat(results.stream().filter(OrderResult::replayed)).hasSize(9);
        assertThat(results.stream().filter(result -> !result.replayed())).hasSize(1);
        assertThat(results).extracting(OrderResult::orderId).containsOnly(results.get(0).orderId());
        assertThat(results).allSatisfy(result -> assertThat(result.items())
            .extracting(OrderResult.Item::menuId)
            .containsExactly(1L, 2L));

        long orderId = results.get(0).orderId();
        assertThat(count("orders")).isEqualTo(1);
        assertThat(count("order_item")).isEqualTo(2);
        assertThat(countWhere("point_history", "type = 'USE'")).isEqualTo(1);
        assertThat(walletBalance()).isEqualTo(16_000L);
        assertThat(outboxStatus()).isEqualTo("PENDING");
        assertThat(outboxAttemptCount()).isZero();

        String eventId = outboxEventId();
        JsonNode payload = objectMapper.readTree(outboxPayload());
        assertThat(payload.path("orderId").asLong()).isEqualTo(orderId);
        assertThat(payload.path("data").path("totalAmount").asLong()).isEqualTo(14_000L);
        assertThat(payload.path("data").path("items"))
            .extracting(item -> item.path("menuId").asLong())
            .containsExactly(1L, 2L);

        publishToMockDataPlatform();

        assertThat(outboxStatus()).isEqualTo("PUBLISHED");
        assertThat(outboxAttemptCount()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
            "SELECT published_at IS NOT NULL FROM order_event_outbox WHERE event_id = ?", Boolean.class, eventId
        )).isTrue();
        assertThat(receivedEventCount(eventId)).isEqualTo(1);

        mockDataPlatformService.receive(eventId, outboxPayload());
        assertThat(receivedEventCount(eventId)).isEqualTo(1);

        clock.set(ORDER_OCCURRED_AT.plusSeconds(1));
        List<PopularMenu> popularMenus = popularMenuQueryService.getPopularMenus().items();

        assertThat(popularMenus)
            .extracting(PopularMenu::menuId, PopularMenu::totalQuantity)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(1L, 2L),
                org.assertj.core.groups.Tuple.tuple(2L, 1L)
            );
    }

    @Test
    void rejectedOrderCreatesNoOutboxDeliveryOrPopularityGhost() {
        pointChargeService.charge(new PointChargeCommand(USER_ID, 10_000L, "s11-insufficient-charge"));

        assertThatThrownBy(() -> orderService.create(orderCommand()))
            .isInstanceOfSatisfying(DomainException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_POINTS));

        assertThat(count("orders")).isZero();
        assertThat(count("order_item")).isZero();
        assertThat(countWhere("point_history", "type = 'USE'")).isZero();
        assertThat(count("order_event_outbox")).isZero();
        assertThat(count("mock_data_platform_received_event")).isZero();
        assertThat(walletBalance()).isEqualTo(10_000L);

        clock.set(ORDER_OCCURRED_AT.plusSeconds(1));
        assertThat(popularMenuQueryService.getPopularMenus().items()).isEmpty();
    }

    private List<OrderResult> createSameOrderConcurrently() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(10);
        try {
            List<Future<OrderResult>> futures = new ArrayList<>();
            for (int index = 0; index < 10; index++) {
                boolean reverseInputOrder = index % 2 == 0;
                futures.add(workers.submit(() -> {
                    start.await();
                    return orderService.create(orderCommand(reverseInputOrder));
                }));
            }
            start.countDown();

            List<OrderResult> results = new ArrayList<>();
            for (Future<OrderResult> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            workers.shutdownNow();
        }
    }

    private void publishToMockDataPlatform() {
        DataPlatformClient client = event -> {
            mockDataPlatformService.receive(event.eventId(), event.payload());
            return DeliveryResult.published();
        };
        ExecutorService workerExecutor = Executors.newSingleThreadExecutor();
        try {
            OutboxPublisher publisher = new OutboxPublisher(
                outboxPublisherProperties,
                claimTransactionExecutor,
                stateTransactionExecutor,
                retryPolicy,
                client,
                workerExecutor
            );
            List<CompletableFuture<Void>> futures = publisher.publishDueEvents();
            assertThat(futures).hasSize(1);
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        } finally {
            workerExecutor.shutdownNow();
        }
    }

    private OrderCommand orderCommand() {
        return orderCommand(false);
    }

    private OrderCommand orderCommand(boolean reverseInputOrder) {
        List<OrderCommand.Item> items = reverseInputOrder
            ? List.of(new OrderCommand.Item(2L, 1), new OrderCommand.Item(1L, 2))
            : List.of(new OrderCommand.Item(1L, 2), new OrderCommand.Item(2L, 1));
        return new OrderCommand(USER_ID, items, ORDER_KEY);
    }

    private void ensureAssignmentMenus() {
        jdbcTemplate.update("""
            INSERT INTO menu (id, name, price, status, created_at, updated_at)
            VALUES (1, '아메리카노', 4500, 'ON_SALE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
            ON DUPLICATE KEY UPDATE name = VALUES(name), price = VALUES(price), status = VALUES(status),
                                    updated_at = VALUES(updated_at)
            """);
        jdbcTemplate.update("""
            INSERT INTO menu (id, name, price, status, created_at, updated_at)
            VALUES (2, '카페라테', 5000, 'ON_SALE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
            ON DUPLICATE KEY UPDATE name = VALUES(name), price = VALUES(price), status = VALUES(status),
                                    updated_at = VALUES(updated_at)
            """);
    }

    private void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM mock_data_platform_received_event");
        jdbcTemplate.update("DELETE FROM order_event_outbox");
        jdbcTemplate.update("DELETE FROM point_history");
        jdbcTemplate.update("DELETE FROM order_item");
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update("UPDATE point_wallet SET balance = 0, updated_at = UTC_TIMESTAMP(6)");
    }

    private int count(String table) {
        Integer value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return value == null ? 0 : value;
    }

    private int countWhere(String table, String predicate) {
        Integer value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + predicate, Integer.class);
        return value == null ? 0 : value;
    }

    private long walletBalance() {
        Long balance = jdbcTemplate.queryForObject(
            "SELECT balance FROM point_wallet WHERE user_id = ?", Long.class, USER_ID
        );
        return balance == null ? 0L : balance;
    }

    private String outboxStatus() {
        return jdbcTemplate.queryForObject("SELECT status FROM order_event_outbox", String.class);
    }

    private int outboxAttemptCount() {
        Integer attemptCount = jdbcTemplate.queryForObject("SELECT attempt_count FROM order_event_outbox", Integer.class);
        return attemptCount == null ? 0 : attemptCount;
    }

    private String outboxEventId() {
        return jdbcTemplate.queryForObject("SELECT event_id FROM order_event_outbox", String.class);
    }

    private String outboxPayload() {
        return jdbcTemplate.queryForObject("SELECT payload FROM order_event_outbox", String.class);
    }

    private int receivedEventCount(String eventId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM mock_data_platform_received_event WHERE event_id = ?", Integer.class, eventId
        );
        return count == null ? 0 : count;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MutableClockConfiguration {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(ORDER_OCCURRED_AT);
        }
    }

    static final class MutableClock extends Clock {

        private volatile Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        void set(Instant instant) {
            this.instant = instant;
        }
    }
}
