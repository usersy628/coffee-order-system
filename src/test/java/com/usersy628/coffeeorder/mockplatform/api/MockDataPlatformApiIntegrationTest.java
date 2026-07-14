package com.usersy628.coffeeorder.mockplatform.api;

import com.usersy628.coffeeorder.outbox.application.OutboxClaimTransactionExecutor;
import com.usersy628.coffeeorder.outbox.application.OutboxStateTransactionExecutor;
import com.usersy628.coffeeorder.outbox.domain.ClaimedOutboxEvent;
import com.usersy628.coffeeorder.support.testcontainers.MySqlIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MySqlIntegrationTest
class MockDataPlatformApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private OutboxClaimTransactionExecutor claimTransactionExecutor;

	@Autowired
	private OutboxStateTransactionExecutor stateTransactionExecutor;

	private final List<Long> createdOrderIds = new ArrayList<>();

	@BeforeEach
	void clearReceivedEvents() {
		jdbcTemplate.update("DELETE FROM mock_data_platform_received_event");
	}

	@AfterEach
	void cleanUpOutboxEvents() {
		for (Long orderId : createdOrderIds) {
			jdbcTemplate.update("DELETE FROM order_event_outbox WHERE order_id = ?", orderId);
			jdbcTemplate.update("DELETE FROM orders WHERE id = ?", orderId);
		}
	}

	@Test
	void receivesAnEventAndReturnsOk() throws Exception {
		send("7e66b7f1-4f58-4f4d-b6fb-8e1b55ebd2f1");

		Integer receivedCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM mock_data_platform_received_event", Integer.class
		);
		assertThat(receivedCount).isEqualTo(1);
	}

	@Test
	void rejectsAnEventWithoutATextEventId() throws Exception {
		mockMvc.perform(post("/internal/mock-data-platform/events")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"eventType\":\"ORDER_COMPLETED\"}"))
			.andExpect(status().isBadRequest());

		Integer receivedCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM mock_data_platform_received_event", Integer.class
		);
		assertThat(receivedCount).isZero();
	}

	@Test
	void acceptsTheSameEventTwiceButStoresItOnlyOnce() throws Exception {
		String eventId = "6fc1ac14-8658-4a5f-a84b-8d0008dbe93e";

		send(eventId);
		send(eventId);

		Integer receivedCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM mock_data_platform_received_event WHERE event_id = ?", Integer.class, eventId
		);
		assertThat(receivedCount).isEqualTo(1);
	}

	@Test
	void acceptsConcurrentDuplicateEventsButStoresOnlyOneRow() throws Exception {
		String eventId = "3d2de6ae-7da5-4927-91b3-83d9fe5ac540";
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService workers = Executors.newFixedThreadPool(2);
		try {
			Future<Void> first = workers.submit(() -> sendWhenStarted(eventId, ready, start));
			Future<Void> second = workers.submit(() -> sendWhenStarted(eventId, ready, start));

			assertThat(ready.await(2, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			first.get();
			second.get();
		} finally {
			workers.shutdownNow();
		}

		Integer receivedCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM mock_data_platform_received_event WHERE event_id = ?", Integer.class, eventId
		);
		assertThat(receivedCount).isEqualTo(1);
	}

	@Test
	void keepsOneMockRowWhenLeaseRecoveryReplaysAnAlreadyReceivedEvent() throws Exception {
		String eventId = "ddb6c03d-e767-4813-a760-78ef3c3e0aa3";
		long outboxId = insertOldestPendingOutboxEvent(eventId);

		ClaimedOutboxEvent firstClaim = claimTransactionExecutor.reclaimAndClaim(1).get(0);
		send(eventId);
		jdbcTemplate.update("""
			UPDATE order_event_outbox
			SET locked_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 31 SECOND)
			WHERE id = ?
			""", outboxId);

		ClaimedOutboxEvent replayClaim = claimTransactionExecutor.reclaimAndClaim(1).get(0);
		send(eventId);

		assertThat(firstClaim.eventId()).isEqualTo(eventId);
		assertThat(replayClaim.eventId()).isEqualTo(eventId);
		assertThat(replayClaim.claimToken()).isNotEqualTo(firstClaim.claimToken());
		assertThat(stateTransactionExecutor.markPublished(replayClaim)).isTrue();
		Integer receivedCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM mock_data_platform_received_event WHERE event_id = ?", Integer.class, eventId
		);
		assertThat(receivedCount).isEqualTo(1);
	}

	private Void sendWhenStarted(String eventId, CountDownLatch ready, CountDownLatch start) throws Exception {
		ready.countDown();
		start.await();
		send(eventId);
		return null;
	}

	private long insertOldestPendingOutboxEvent(String eventId) {
		String idempotencyKey = "mock-replay-" + UUID.randomUUID();
		jdbcTemplate.update("""
			INSERT INTO orders (
			    user_id, idempotency_key, request_hash, total_amount, status, created_at, paid_at
			)
			VALUES (1, ?, ?, 4500, 'PAID', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
			""", idempotencyKey, "c".repeat(64));
		Long orderId = jdbcTemplate.queryForObject(
			"SELECT id FROM orders WHERE user_id = 1 AND idempotency_key = ?", Long.class, idempotencyKey
		);
		createdOrderIds.add(orderId);
		jdbcTemplate.update("""
			INSERT INTO order_event_outbox (
			    event_id, order_id, event_type, schema_version, payload, status, attempt_count,
			    next_attempt_at, created_at
			)
			VALUES (?, ?, 'ORDER_COMPLETED', 1, JSON_OBJECT('eventId', ?), 'PENDING', 0,
			        DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 DAY), UTC_TIMESTAMP(6))
			""", eventId, orderId, eventId);
		return jdbcTemplate.queryForObject(
			"SELECT id FROM order_event_outbox WHERE event_id = ?", Long.class, eventId
		);
	}

	private void send(String eventId) throws Exception {
		mockMvc.perform(post("/internal/mock-data-platform/events")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Idempotency-Key", eventId)
				.content("""
					{
					  "eventId": "%s",
					  "eventType": "ORDER_COMPLETED",
					  "schemaVersion": 1,
					  "occurredAt": "2026-07-15T00:00:00Z",
					  "orderId": 101,
					  "data": {"userId": 1, "totalAmount": 4500, "items": []}
					}
					""".formatted(eventId)))
			.andExpect(status().isOk());
	}
}
