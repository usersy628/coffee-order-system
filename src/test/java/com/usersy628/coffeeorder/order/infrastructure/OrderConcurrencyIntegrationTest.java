package com.usersy628.coffeeorder.order.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.sql.DataSource;

import com.usersy628.coffeeorder.global.error.DomainException;
import com.usersy628.coffeeorder.global.error.ErrorCode;
import com.usersy628.coffeeorder.order.application.OrderCommand;
import com.usersy628.coffeeorder.order.application.OrderResult;
import com.usersy628.coffeeorder.order.application.OrderService;
import com.usersy628.coffeeorder.support.testcontainers.MySqlIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@MySqlIntegrationTest
class OrderConcurrencyIntegrationTest {

	@Autowired
	private OrderService orderService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DataSource dataSource;

	private ExecutorService executorService;

	@BeforeEach
	void setUp() {
		resetOrders();
		jdbcTemplate.update("UPDATE point_wallet SET balance = 300000, updated_at = UTC_TIMESTAMP(6)");
		jdbcTemplate.update("UPDATE menu SET price = 1 WHERE id = 1");
		executorService = Executors.newFixedThreadPool(20);
	}

	@AfterEach
	void tearDown() {
		executorService.shutdownNow();
		resetOrders();
		jdbcTemplate.update("UPDATE menu SET price = 4500 WHERE id = 1");
		jdbcTemplate.update("UPDATE point_wallet SET balance = 0, updated_at = UTC_TIMESTAMP(6)");
	}

	@Test
	void appliesOneOrderForOneHundredConcurrentRequestsWithTheSameKey() throws Exception {
		List<OrderResult> results = runConcurrently(100, index -> command("same-order-key"));

		assertThat(results).hasSize(100);
		assertThat(results.stream().filter(result -> !result.replayed()).count()).isEqualTo(1);
		assertThat(results.stream().filter(OrderResult::replayed).count()).isEqualTo(99);
		assertThat(count("orders")).isEqualTo(1);
		assertThat(count("point_history")).isEqualTo(1);
		assertThat(count("order_event_outbox")).isEqualTo(1);
		assertThat(walletBalance()).isEqualTo(299999L);
	}

	@Test
	void serializesOneHundredDifferentOrdersWithoutLostPointUpdates() throws Exception {
		List<OrderResult> results = runConcurrently(100, index -> command("different-order-" + index));

		assertThat(results).hasSize(100);
		assertThat(results).allMatch(result -> !result.replayed());
		assertThat(count("orders")).isEqualTo(100);
		assertThat(count("point_history")).isEqualTo(100);
		assertThat(count("order_event_outbox")).isEqualTo(100);
		assertThat(walletBalance()).isEqualTo(299900L);
	}

	@Test
	void returnsConcurrentRequestTimeoutAfterRealMySqlLockWaitRetriesAreExhausted() throws Exception {
		try (Connection lockConnection = dataSource.getConnection()) {
			lockConnection.setAutoCommit(false);
			try (PreparedStatement statement = lockConnection.prepareStatement(
				"SELECT user_id FROM point_wallet WHERE user_id = 1 FOR UPDATE"
			)) {
				statement.executeQuery();

				assertThatThrownBy(() -> orderService.create(command("lock-timeout-order")))
					.isInstanceOfSatisfying(DomainException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONCURRENT_REQUEST_TIMEOUT));
			} finally {
				lockConnection.rollback();
			}
		}

		assertThat(count("orders")).isZero();
		assertThat(walletBalance()).isEqualTo(300000L);
	}

	@Test
	void rollsBackTheWholeOrderWhenOutboxPersistenceFails() {
		jdbcTemplate.execute("RENAME TABLE order_event_outbox TO order_event_outbox_unavailable");
		try {
			assertThatThrownBy(() -> orderService.create(command("outbox-failure-order")))
				.isInstanceOf(RuntimeException.class);
		} finally {
			jdbcTemplate.execute("RENAME TABLE order_event_outbox_unavailable TO order_event_outbox");
		}

		assertThat(count("orders")).isZero();
		assertThat(count("order_item")).isZero();
		assertThat(count("point_history")).isZero();
		assertThat(count("order_event_outbox")).isZero();
		assertThat(walletBalance()).isEqualTo(300000L);
	}

	private List<OrderResult> runConcurrently(
		int requestCount,
		java.util.function.IntFunction<OrderCommand> commandFactory
	) throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		List<Future<OrderResult>> futures = new ArrayList<>();
		for (int index = 0; index < requestCount; index++) {
			int requestIndex = index;
			futures.add(executorService.submit(() -> {
				start.await();
				return orderService.create(commandFactory.apply(requestIndex));
			}));
		}
		start.countDown();

		List<OrderResult> results = new ArrayList<>();
		for (Future<OrderResult> future : futures) {
			results.add(future.get());
		}
		return results;
	}

	private OrderCommand command(String key) {
		return new OrderCommand(1L, List.of(new OrderCommand.Item(1L, 1)), key);
	}

	private void resetOrders() {
		jdbcTemplate.update("DELETE FROM order_event_outbox");
		jdbcTemplate.update("DELETE FROM point_history");
		jdbcTemplate.update("DELETE FROM order_item");
		jdbcTemplate.update("DELETE FROM orders");
	}

	private int count(String table) {
		Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
		return count == null ? 0 : count;
	}

	private long walletBalance() {
		Long balance = jdbcTemplate.queryForObject(
			"SELECT balance FROM point_wallet WHERE user_id = 1", Long.class);
		return balance == null ? 0 : balance;
	}
}
