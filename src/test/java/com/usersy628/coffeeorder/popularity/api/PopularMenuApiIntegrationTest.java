package com.usersy628.coffeeorder.popularity.api;

import com.usersy628.coffeeorder.support.testcontainers.MySqlIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MySqlIntegrationTest
@Transactional
@Import(PopularMenuApiIntegrationTest.FixedClockConfiguration.class)
class PopularMenuApiIntegrationTest {

	private static final Instant RAW_NOW = Instant.parse("2026-07-15T00:00:00.123456789Z");
	private static final Instant TO = RAW_NOW.truncatedTo(ChronoUnit.MICROS);
	private static final Instant FROM = TO.minus(168, ChronoUnit.HOURS);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private CountingClock clock;

	@BeforeEach
	void resetData() {
		clock.reset();
		jdbcTemplate.update("DELETE FROM order_event_outbox");
		jdbcTemplate.update("DELETE FROM point_history");
		jdbcTemplate.update("DELETE FROM order_item");
		jdbcTemplate.update("DELETE FROM orders");
		jdbcTemplate.update("DELETE FROM menu");
	}

	@Test
	void returnsTheTopThreeForTheMicrosTruncatedHalfOpenWindow() throws Exception {
		insertMenu(10L, "current-americano", "ON_SALE");
		insertMenu(20L, "current-latte", "STOPPED");
		insertMenu(30L, "current-mocha", "ON_SALE");
		insertMenu(40L, "current-tea", "ON_SALE");

		long atStart = insertOrder("at-start", FROM);
		insertItem(atStart, 10L, "historical-americano", 4);
		insertItem(atStart, 20L, "historical-latte", 2);
		long justBeforeEnd = insertOrder("just-before-end", TO.minus(1, ChronoUnit.MICROS));
		insertItem(justBeforeEnd, 10L, "historical-americano", 3);
		insertItem(justBeforeEnd, 20L, "historical-latte", 5);
		insertItem(justBeforeEnd, 30L, "historical-mocha", 5);
		insertItem(justBeforeEnd, 40L, "historical-tea", 4);
		long beforeStart = insertOrder("before-start", FROM.minus(1, ChronoUnit.MICROS));
		insertItem(beforeStart, 40L, "historical-tea", 100);
		long atEnd = insertOrder("at-end", TO);
		insertItem(atEnd, 30L, "historical-mocha", 100);

		mockMvc.perform(get("/api/menus/popular"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(header().string("X-Trace-Id", matchesPattern("[0-9a-f]{32}")))
			.andExpect(jsonPath("$.from").value("2026-07-08T09:00:00.123456+09:00"))
			.andExpect(jsonPath("$.to").value("2026-07-15T09:00:00.123456+09:00"))
			.andExpect(jsonPath("$.items.length()").value(3))
			.andExpect(jsonPath("$.items[0].rank").value(1))
			.andExpect(jsonPath("$.items[0].menuId").value(10))
			.andExpect(jsonPath("$.items[0].menuName").value("current-americano"))
			.andExpect(jsonPath("$.items[0].totalQuantity").value(7))
			.andExpect(jsonPath("$.items[1].rank").value(2))
			.andExpect(jsonPath("$.items[1].menuId").value(20))
			.andExpect(jsonPath("$.items[1].menuName").value("current-latte"))
			.andExpect(jsonPath("$.items[1].totalQuantity").value(7))
			.andExpect(jsonPath("$.items[2].rank").value(3))
			.andExpect(jsonPath("$.items[2].menuId").value(30))
			.andExpect(jsonPath("$.items[2].totalQuantity").value(5));

		assertThat(clock.instantCallCount()).isEqualTo(1);
	}

	@Test
	void returnsAnEmptyItemsArrayForTheSameClockWindowWhenNoOrdersExist() throws Exception {
		mockMvc.perform(get("/api/menus/popular"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.from").value("2026-07-08T09:00:00.123456+09:00"))
			.andExpect(jsonPath("$.to").value("2026-07-15T09:00:00.123456+09:00"))
			.andExpect(jsonPath("$.items").isArray())
			.andExpect(jsonPath("$.items.length()").value(0));

		assertThat(clock.instantCallCount()).isEqualTo(1);
	}

	private void insertMenu(long id, String name, String status) {
		jdbcTemplate.update("""
			INSERT INTO menu (id, name, price, status, created_at, updated_at)
			VALUES (?, ?, 1000, ?, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
			""", id, name, status);
	}

	private long insertOrder(String suffix, Instant paidAt) {
		String idempotencyKey = "popular-" + suffix;
		jdbcTemplate.update("""
			INSERT INTO orders (user_id, idempotency_key, request_hash, total_amount, status, created_at, paid_at)
			VALUES (1, ?, ?, 1000, 'PAID', ?, ?)
			""", idempotencyKey, "a".repeat(64), Timestamp.from(paidAt), Timestamp.from(paidAt));
		return jdbcTemplate.queryForObject(
			"SELECT id FROM orders WHERE user_id = 1 AND idempotency_key = ?", Long.class, idempotencyKey
		);
	}

	private void insertItem(long orderId, long menuId, String menuName, int quantity) {
		jdbcTemplate.update("""
			INSERT INTO order_item (order_id, menu_id, menu_name, unit_price, quantity, line_amount)
			VALUES (?, ?, ?, 1000, ?, ?)
			""", orderId, menuId, menuName, quantity, 1000L * quantity);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FixedClockConfiguration {

		@Bean
		@Primary
		CountingClock fixedClock() {
			return new CountingClock(RAW_NOW);
		}
	}

	static final class CountingClock extends Clock {

		private final Instant instant;
		private final AtomicInteger instantCallCount = new AtomicInteger();

		private CountingClock(Instant instant) {
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
			instantCallCount.incrementAndGet();
			return instant;
		}

		void reset() {
			instantCallCount.set(0);
		}

		int instantCallCount() {
			return instantCallCount.get();
		}
	}
}
