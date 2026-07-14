package com.usersy628.coffeeorder.outbox.domain;

public record ClaimedOutboxEvent(
	long id,
	String eventId,
	String payload,
	int attemptCount,
	String claimToken
) {
}
