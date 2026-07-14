package com.usersy628.coffeeorder.outbox.application;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class OutboxRetryPolicy {

	private final OutboxPublisherProperties properties;

	public OutboxRetryPolicy(OutboxPublisherProperties properties) {
		this.properties = properties;
	}

	public Duration nextRetryDelay(int attemptCount) {
		if (attemptCount < 1 || attemptCount >= properties.getMaxAttempts()) {
			throw new IllegalArgumentException("Only retryable attempts below the maximum can receive a next delay");
		}
		long baseMillis = Math.multiplyExact(
			properties.getInitialBackoff().toMillis(), 1L << (attemptCount - 1)
		);
		double jitterFactor = properties.getJitterFactor();
		double multiplier = jitterFactor == 0.0
			? 1.0
			: 1.0 + ThreadLocalRandom.current().nextDouble(-jitterFactor, jitterFactor);
		return Duration.ofMillis(Math.max(1L, Math.round(baseMillis * multiplier)));
	}
}
