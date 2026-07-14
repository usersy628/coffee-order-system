package com.usersy628.coffeeorder.outbox.application;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxRetryPolicyTest {

	@Test
	void appliesTheApprovedExponentialBackoffWithinTheJitterRange() {
		OutboxPublisherProperties properties = new OutboxPublisherProperties();
		OutboxRetryPolicy policy = new OutboxRetryPolicy(properties);

		long[] baseMillis = {1_000L, 2_000L, 4_000L, 8_000L, 16_000L};
		for (int attemptCount = 1; attemptCount <= 5; attemptCount++) {
			for (int sample = 0; sample < 20; sample++) {
				Duration delay = policy.nextRetryDelay(attemptCount);
				assertThat(delay.toMillis())
					.isBetween(Math.round(baseMillis[attemptCount - 1] * 0.8), Math.round(baseMillis[attemptCount - 1] * 1.2));
			}
		}
	}

	@Test
	void doesNotScheduleASeventhAttempt() {
		OutboxRetryPolicy policy = new OutboxRetryPolicy(new OutboxPublisherProperties());

		assertThatThrownBy(() -> policy.nextRetryDelay(0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> policy.nextRetryDelay(6)).isInstanceOf(IllegalArgumentException.class);
	}
}
