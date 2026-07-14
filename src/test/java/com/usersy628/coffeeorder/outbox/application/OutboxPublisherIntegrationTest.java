package com.usersy628.coffeeorder.outbox.application;

import com.usersy628.coffeeorder.outbox.domain.DeliveryResult;
import com.usersy628.coffeeorder.support.testcontainers.MySqlIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@MySqlIntegrationTest
class OutboxPublisherIntegrationTest {

	@Autowired
	private OutboxPublisherProperties properties;

	@Autowired
	private OutboxClaimTransactionExecutor claimTransactionExecutor;

	@Autowired
	private OutboxStateTransactionExecutor stateTransactionExecutor;

	@Autowired
	private OutboxRetryPolicy retryPolicy;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private final List<Long> createdOrderIds = new ArrayList<>();

	@AfterEach
	void cleanUp() {
		for (Long orderId : createdOrderIds) {
			jdbcTemplate.update("DELETE FROM order_event_outbox WHERE order_id = ?", orderId);
			jdbcTemplate.update("DELETE FROM orders WHERE id = ?", orderId);
		}
	}

	@Test
	void marksA2xxDeliveryAsPublishedOutsideAnyDatabaseTransaction() {
		long outboxId = insertPendingEvent(0);
		AtomicBoolean clientSawTransaction = new AtomicBoolean(true);
		DataPlatformClient client = event -> {
			clientSawTransaction.set(TransactionSynchronizationManager.isActualTransactionActive());
			return DeliveryResult.published();
		};

		publish(client);

		assertThat(clientSawTransaction).isFalse();
		assertThat(statusOf(outboxId)).isEqualTo("PUBLISHED");
		assertThat(attemptCountOf(outboxId)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
			"SELECT published_at IS NOT NULL FROM order_event_outbox WHERE id = ?", Boolean.class, outboxId
		)).isTrue();
	}

	@Test
	void marksA4xxDeliveryAsFailedWithoutSchedulingARetry() {
		long outboxId = insertPendingEvent(0);

		publish(event -> DeliveryResult.permanentFailure("HTTP_400"));

		assertThat(statusOf(outboxId)).isEqualTo("FAILED");
		assertThat(attemptCountOf(outboxId)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
			"SELECT next_attempt_at IS NULL FROM order_event_outbox WHERE id = ?", Boolean.class, outboxId
		)).isTrue();
		assertThat(jdbcTemplate.queryForObject(
			"SELECT last_error FROM order_event_outbox WHERE id = ?", String.class, outboxId
		)).isEqualTo("HTTP_400");
	}

	@Test
	void schedulesA5xxDeliveryForOutboxOwnedRetry() {
		long outboxId = insertPendingEvent(0);

		publish(event -> DeliveryResult.retryableFailure("HTTP_500"));

		assertThat(statusOf(outboxId)).isEqualTo("PENDING");
		assertThat(attemptCountOf(outboxId)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
			"SELECT next_attempt_at IS NOT NULL FROM order_event_outbox WHERE id = ?", Boolean.class, outboxId
		)).isTrue();
		assertThat(jdbcTemplate.queryForObject(
			"SELECT last_error FROM order_event_outbox WHERE id = ?", String.class, outboxId
		)).isEqualTo("HTTP_500");
	}

	@Test
	void retriesTheSameEventSixTimesBeforeMarkingItFailed() {
		long outboxId = insertPendingEvent(0);
		AtomicInteger deliveryCalls = new AtomicInteger();
		DataPlatformClient retryableClient = event -> {
			deliveryCalls.incrementAndGet();
			return DeliveryResult.retryableFailure("HTTP_TIMEOUT");
		};

		for (int attempt = 1; attempt <= 6; attempt++) {
			publish(retryableClient);

			assertThat(attemptCountOf(outboxId)).isEqualTo(attempt);
			if (attempt < 6) {
				assertThat(statusOf(outboxId)).isEqualTo("PENDING");
				jdbcTemplate.update(
					"UPDATE order_event_outbox SET next_attempt_at = UTC_TIMESTAMP(6) WHERE id = ?", outboxId
				);
			}
		}

		assertThat(deliveryCalls).hasValue(6);
		assertThat(statusOf(outboxId)).isEqualTo("FAILED");
		assertThat(jdbcTemplate.queryForObject(
			"SELECT last_error FROM order_event_outbox WHERE id = ?", String.class, outboxId
		)).isEqualTo("HTTP_TIMEOUT");
	}

	@Test
	void claimsNoMoreEventsThanAvailableWorkerSlots() throws Exception {
		long firstOutboxId = insertPendingEvent(0);
		long secondOutboxId = insertPendingEvent(0);
		jdbcTemplate.update("""
			UPDATE order_event_outbox
			SET next_attempt_at = CASE id
				WHEN ? THEN DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 SECOND)
				WHEN ? THEN DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 SECOND)
			END
			WHERE id IN (?, ?)
			""", firstOutboxId, secondOutboxId, firstOutboxId, secondOutboxId);

		OutboxPublisherProperties singleWorkerProperties = new OutboxPublisherProperties();
		singleWorkerProperties.setBatchSize(10);
		singleWorkerProperties.setMaxConcurrency(1);
		CountDownLatch deliveryStarted = new CountDownLatch(1);
		CountDownLatch allowDelivery = new CountDownLatch(1);
		DataPlatformClient blockingClient = event -> {
			deliveryStarted.countDown();
			try {
				allowDelivery.await();
				return DeliveryResult.published();
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				return DeliveryResult.retryableFailure("HTTP_INTERRUPTED");
			}
		};
		ExecutorService workers = Executors.newFixedThreadPool(2);
		try {
			OutboxPublisher publisher = new OutboxPublisher(
				singleWorkerProperties, claimTransactionExecutor, stateTransactionExecutor, retryPolicy, blockingClient, workers
			);
			List<CompletableFuture<Void>> firstPoll = publisher.publishDueEvents();

			assertThat(deliveryStarted.await(2, TimeUnit.SECONDS)).isTrue();
			assertThat(firstPoll).hasSize(1);
			assertThat(publisher.publishDueEvents()).isEmpty();
			assertThat(statusOf(firstOutboxId)).isEqualTo("PROCESSING");
			assertThat(statusOf(secondOutboxId)).isEqualTo("PENDING");

			allowDelivery.countDown();
			CompletableFuture.allOf(firstPoll.toArray(CompletableFuture[]::new)).join();
			assertThat(statusOf(firstOutboxId)).isEqualTo("PUBLISHED");
			assertThat(attemptCountOf(secondOutboxId)).isZero();
		} finally {
			allowDelivery.countDown();
			workers.shutdownNow();
		}
	}

	private void publish(DataPlatformClient client) {
		ExecutorService workerExecutor = Executors.newFixedThreadPool(1);
		try {
			OutboxPublisher publisher = new OutboxPublisher(
				properties, claimTransactionExecutor, stateTransactionExecutor, retryPolicy, client, workerExecutor
			);
			List<CompletableFuture<Void>> futures = publisher.publishDueEvents();
			CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
		} finally {
			workerExecutor.shutdownNow();
		}
	}

	private long insertPendingEvent(int attemptCount) {
		long orderId = insertOrder();
		String eventId = UUID.randomUUID().toString();
		jdbcTemplate.update("""
			INSERT INTO order_event_outbox (
			    event_id, order_id, event_type, schema_version, payload, status, attempt_count,
			    next_attempt_at, created_at
			)
			VALUES (?, ?, 'ORDER_COMPLETED', 1, JSON_OBJECT('eventId', ?), 'PENDING', ?,
			        UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
			""", eventId, orderId, eventId, attemptCount);
		return jdbcTemplate.queryForObject(
			"SELECT id FROM order_event_outbox WHERE event_id = ?", Long.class, eventId
		);
	}

	private long insertOrder() {
		String idempotencyKey = "outbox-publisher-" + UUID.randomUUID();
		jdbcTemplate.update("""
			INSERT INTO orders (
			    user_id, idempotency_key, request_hash, total_amount, status, created_at, paid_at
			)
			VALUES (1, ?, ?, 4500, 'PAID', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
			""", idempotencyKey, "b".repeat(64));
		Long orderId = jdbcTemplate.queryForObject(
			"SELECT id FROM orders WHERE user_id = 1 AND idempotency_key = ?", Long.class, idempotencyKey
		);
		createdOrderIds.add(orderId);
		return orderId;
	}

	private String statusOf(long outboxId) {
		return jdbcTemplate.queryForObject(
			"SELECT status FROM order_event_outbox WHERE id = ?", String.class, outboxId
		);
	}

	private int attemptCountOf(long outboxId) {
		return jdbcTemplate.queryForObject(
			"SELECT attempt_count FROM order_event_outbox WHERE id = ?", Integer.class, outboxId
		);
	}
}
