package com.usersy628.coffeeorder.outbox.infrastructure;

import com.usersy628.coffeeorder.outbox.application.OutboxClaimTransactionExecutor;
import com.usersy628.coffeeorder.outbox.application.OutboxStateTransactionExecutor;
import com.usersy628.coffeeorder.outbox.domain.ClaimedOutboxEvent;
import com.usersy628.coffeeorder.support.testcontainers.MySqlIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@MySqlIntegrationTest
class OutboxClaimIntegrationTest {

	@Autowired
	private OutboxClaimTransactionExecutor claimTransactionExecutor;

	@Autowired
	private OutboxStateTransactionExecutor stateTransactionExecutor;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DataSource dataSource;

	private final List<Long> createdOrderIds = new ArrayList<>();

	@AfterEach
	void cleanUp() {
		for (Long orderId : createdOrderIds) {
			jdbcTemplate.update("DELETE FROM order_event_outbox WHERE order_id = ?", orderId);
			jdbcTemplate.update("DELETE FROM orders WHERE id = ?", orderId);
		}
	}

	@Test
	void twoWorkersDoNotClaimTheSamePendingEvent() throws Exception {
		insertPendingEvent();
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService workers = Executors.newFixedThreadPool(2);
		try {
			Future<List<ClaimedOutboxEvent>> first = workers.submit(() -> {
				start.await();
				return claimTransactionExecutor.reclaimAndClaim(1);
			});
			Future<List<ClaimedOutboxEvent>> second = workers.submit(() -> {
				start.await();
				return claimTransactionExecutor.reclaimAndClaim(1);
			});

			start.countDown();
			int totalClaims = first.get().size() + second.get().size();

			assertThat(totalClaims).isEqualTo(1);
		} finally {
			workers.shutdownNow();
		}
	}

	@Test
	void skipsALockedDueEventWithoutWaitingForItsTransaction() throws Exception {
		long lockedOutboxId = insertPendingEvent();
		long availableOutboxId = insertPendingEvent();
		jdbcTemplate.update("""
			UPDATE order_event_outbox
			SET next_attempt_at = CASE id
				WHEN ? THEN DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 SECOND)
				WHEN ? THEN DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 SECOND)
			END
			WHERE id IN (?, ?)
			""", lockedOutboxId, availableOutboxId, lockedOutboxId, availableOutboxId);

		try (Connection lockHolder = dataSource.getConnection()) {
			lockHolder.setAutoCommit(false);
			try (PreparedStatement statement = lockHolder.prepareStatement(
				"SELECT id FROM order_event_outbox WHERE id = ? FOR UPDATE"
			)) {
				statement.setLong(1, lockedOutboxId);
				statement.executeQuery();
			}

			ExecutorService worker = Executors.newSingleThreadExecutor();
			try {
				Future<List<ClaimedOutboxEvent>> claimedFuture = worker.submit(
					() -> claimTransactionExecutor.reclaimAndClaim(1)
				);

				List<ClaimedOutboxEvent> claimed = claimedFuture.get(2, TimeUnit.SECONDS);

				assertThat(claimed).extracting(ClaimedOutboxEvent::id).containsExactly(availableOutboxId);
				assertThat(statusOf(lockedOutboxId)).isEqualTo("PENDING");
			} finally {
				worker.shutdownNow();
			}
			lockHolder.rollback();
		}
	}

	@Test
	void claimsDueEventsInNextAttemptOrderAndRespectsTheBatchLimit() {
		long newest = insertPendingEvent();
		long oldest = insertPendingEvent();
		long middle = insertPendingEvent();
		jdbcTemplate.update("""
			UPDATE order_event_outbox
			SET next_attempt_at = CASE id
				WHEN ? THEN DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 SECOND)
				WHEN ? THEN DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 SECOND)
				WHEN ? THEN DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 SECOND)
			END
			WHERE id IN (?, ?, ?)
			""", newest, oldest, middle, newest, oldest, middle);

		List<ClaimedOutboxEvent> claimed = claimTransactionExecutor.reclaimAndClaim(2);

		assertThat(claimed).extracting(ClaimedOutboxEvent::id).containsExactly(oldest, middle);
		assertThat(statusOf(newest)).isEqualTo("PENDING");
		assertThat(statusOf(oldest)).isEqualTo("PROCESSING");
		assertThat(statusOf(middle)).isEqualTo("PROCESSING");
	}

	@Test
	void reclaimsAnExpiredLeaseAndRejectsEveryUpdateFromTheOldClaimToken() {
		long outboxId = insertPendingEvent();
		ClaimedOutboxEvent firstClaim = claimTransactionExecutor.reclaimAndClaim(1).get(0);
		jdbcTemplate.update("""
			UPDATE order_event_outbox
			SET locked_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 31 SECOND)
			WHERE id = ?
			""", outboxId);

		ClaimedOutboxEvent secondClaim = claimTransactionExecutor.reclaimAndClaim(1).get(0);

		assertThat(secondClaim.claimToken()).isNotEqualTo(firstClaim.claimToken());
		assertThat(stateTransactionExecutor.markPublished(firstClaim)).isFalse();
		assertThat(stateTransactionExecutor.scheduleRetry(firstClaim, Duration.ofSeconds(1), "HTTP_500")).isFalse();
		assertThat(stateTransactionExecutor.markFailed(firstClaim, "HTTP_400")).isFalse();
		assertThat(stateTransactionExecutor.markPublished(secondClaim)).isTrue();
		assertThat(statusOf(outboxId)).isEqualTo("PUBLISHED");
	}

	@Test
	void turnsAnExpiredSixthAttemptIntoFailedWithoutReclaimingIt() {
		long orderId = insertOrder();
		String eventId = UUID.randomUUID().toString();
		jdbcTemplate.update("""
			INSERT INTO order_event_outbox (
			    event_id, order_id, event_type, schema_version, payload, status, attempt_count,
			    next_attempt_at, locked_at, claim_token, created_at
			)
			VALUES (?, ?, 'ORDER_COMPLETED', 1, JSON_OBJECT('eventId', ?), 'PROCESSING', 6,
			        NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 31 SECOND), ?, UTC_TIMESTAMP(6))
			""", eventId, orderId, eventId, UUID.randomUUID().toString());
		long outboxId = jdbcTemplate.queryForObject(
			"SELECT id FROM order_event_outbox WHERE event_id = ?", Long.class, eventId
		);

		List<ClaimedOutboxEvent> claimed = claimTransactionExecutor.reclaimAndClaim(1);

		assertThat(claimed).isEmpty();
		assertThat(statusOf(outboxId)).isEqualTo("FAILED");
		assertThat(jdbcTemplate.queryForObject(
			"SELECT last_error FROM order_event_outbox WHERE id = ?", String.class, outboxId
		)).isEqualTo("LEASE_EXPIRED_MAX_ATTEMPTS");
	}

	private long insertPendingEvent() {
		long orderId = insertOrder();
		String eventId = UUID.randomUUID().toString();
		jdbcTemplate.update("""
			INSERT INTO order_event_outbox (
			    event_id, order_id, event_type, schema_version, payload, status, attempt_count,
			    next_attempt_at, created_at
			)
			VALUES (?, ?, 'ORDER_COMPLETED', 1, JSON_OBJECT('eventId', ?), 'PENDING', 0,
			        UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
			""", eventId, orderId, eventId);
		return jdbcTemplate.queryForObject(
			"SELECT id FROM order_event_outbox WHERE event_id = ?", Long.class, eventId
		);
	}

	private long insertOrder() {
		String idempotencyKey = "outbox-claim-" + UUID.randomUUID();
		jdbcTemplate.update("""
			INSERT INTO orders (
			    user_id, idempotency_key, request_hash, total_amount, status, created_at, paid_at
			)
			VALUES (1, ?, ?, 4500, 'PAID', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
			""", idempotencyKey, "a".repeat(64));
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
}
