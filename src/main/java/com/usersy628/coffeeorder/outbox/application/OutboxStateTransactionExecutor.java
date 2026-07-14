package com.usersy628.coffeeorder.outbox.application;

import com.usersy628.coffeeorder.outbox.domain.ClaimedOutboxEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
public class OutboxStateTransactionExecutor {

	private static final int LAST_ERROR_MAX_LENGTH = 1_000;

	private final JdbcTemplate jdbcTemplate;

	public OutboxStateTransactionExecutor(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean markPublished(ClaimedOutboxEvent event) {
		return jdbcTemplate.update("""
			UPDATE order_event_outbox
			SET status = 'PUBLISHED',
			    published_at = UTC_TIMESTAMP(6),
			    next_attempt_at = NULL,
			    locked_at = NULL,
			    claim_token = NULL,
			    failed_at = NULL,
			    last_error = NULL
			WHERE id = ?
			  AND status = 'PROCESSING'
			  AND claim_token = ?
			""", event.id(), event.claimToken()) == 1;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean scheduleRetry(ClaimedOutboxEvent event, Duration delay, String errorSummary) {
		long delayMicros = Math.max(1L, delay.toNanos() / 1_000L);
		return jdbcTemplate.update("""
			UPDATE order_event_outbox
			SET status = 'PENDING',
			    next_attempt_at = DATE_ADD(UTC_TIMESTAMP(6), INTERVAL %d MICROSECOND),
			    locked_at = NULL,
			    claim_token = NULL,
			    published_at = NULL,
			    failed_at = NULL,
			    last_error = ?
			WHERE id = ?
			  AND status = 'PROCESSING'
			  AND claim_token = ?
			""".formatted(delayMicros), summarize(errorSummary), event.id(), event.claimToken()) == 1;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean markFailed(ClaimedOutboxEvent event, String errorSummary) {
		return jdbcTemplate.update("""
			UPDATE order_event_outbox
			SET status = 'FAILED',
			    next_attempt_at = NULL,
			    locked_at = NULL,
			    claim_token = NULL,
			    published_at = NULL,
			    failed_at = UTC_TIMESTAMP(6),
			    last_error = ?
			WHERE id = ?
			  AND status = 'PROCESSING'
			  AND claim_token = ?
			""", summarize(errorSummary), event.id(), event.claimToken()) == 1;
	}

	private String summarize(String errorSummary) {
		String safeSummary = errorSummary == null || errorSummary.isBlank() ? "UNKNOWN_DELIVERY_FAILURE" : errorSummary;
		return safeSummary.substring(0, Math.min(safeSummary.length(), LAST_ERROR_MAX_LENGTH));
	}
}
