package com.usersy628.coffeeorder.outbox.application;

import com.usersy628.coffeeorder.outbox.domain.ClaimedOutboxEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OutboxClaimTransactionExecutor {

	private final JdbcTemplate jdbcTemplate;
	private final OutboxPublisherProperties properties;

	public OutboxClaimTransactionExecutor(JdbcTemplate jdbcTemplate, OutboxPublisherProperties properties) {
		this.jdbcTemplate = jdbcTemplate;
		this.properties = properties;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public List<ClaimedOutboxEvent> reclaimAndClaim(int claimLimit) {
		if (claimLimit < 1) {
			return List.of();
		}
		reclaimExpiredLeases();
		List<Candidate> candidates = jdbcTemplate.query("""
			SELECT id, event_id, payload, attempt_count
			FROM order_event_outbox
			WHERE status = 'PENDING'
			  AND attempt_count < ?
			  AND next_attempt_at <= UTC_TIMESTAMP(6)
			ORDER BY next_attempt_at, id
			LIMIT ?
			FOR UPDATE SKIP LOCKED
			""", (resultSet, rowNum) -> new Candidate(
			resultSet.getLong("id"),
			resultSet.getString("event_id"),
			resultSet.getString("payload"),
			resultSet.getInt("attempt_count")
		), properties.getMaxAttempts(), claimLimit);

		List<ClaimedOutboxEvent> claimedEvents = new ArrayList<>();
		for (Candidate candidate : candidates) {
			String claimToken = UUID.randomUUID().toString();
			int updated = jdbcTemplate.update("""
				UPDATE order_event_outbox
				SET status = 'PROCESSING',
				    attempt_count = attempt_count + 1,
				    next_attempt_at = NULL,
				    locked_at = UTC_TIMESTAMP(6),
				    claim_token = ?,
				    published_at = NULL,
				    failed_at = NULL
				WHERE id = ?
				  AND status = 'PENDING'
				  AND attempt_count < ?
				""", claimToken, candidate.id(), properties.getMaxAttempts());
			if (updated == 1) {
				claimedEvents.add(new ClaimedOutboxEvent(
					candidate.id(), candidate.eventId(), candidate.payload(), candidate.attemptCount() + 1, claimToken
				));
			}
		}
		return claimedEvents;
	}

	private void reclaimExpiredLeases() {
		long leaseMicros = properties.getLeaseTimeout().toNanos() / 1_000L;
		String expiredCondition = "status = 'PROCESSING' AND locked_at < DATE_SUB(UTC_TIMESTAMP(6), INTERVAL %d MICROSECOND)"
			.formatted(leaseMicros);

		jdbcTemplate.update("""
			UPDATE order_event_outbox
			SET status = 'FAILED',
			    next_attempt_at = NULL,
			    locked_at = NULL,
			    claim_token = NULL,
			    published_at = NULL,
			    failed_at = UTC_TIMESTAMP(6),
			    last_error = 'LEASE_EXPIRED_MAX_ATTEMPTS'
			WHERE %s
			  AND attempt_count = ?
			""".formatted(expiredCondition), properties.getMaxAttempts());

		jdbcTemplate.update("""
			UPDATE order_event_outbox
			SET status = 'PENDING',
			    next_attempt_at = UTC_TIMESTAMP(6),
			    locked_at = NULL,
			    claim_token = NULL,
			    published_at = NULL,
			    failed_at = NULL,
			    last_error = 'LEASE_EXPIRED'
			WHERE %s
			  AND attempt_count < ?
			""".formatted(expiredCondition), properties.getMaxAttempts());
	}

	private record Candidate(long id, String eventId, String payload, int attemptCount) {
	}
}
