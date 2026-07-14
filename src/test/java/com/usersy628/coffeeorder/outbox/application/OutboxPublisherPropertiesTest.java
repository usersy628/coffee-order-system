package com.usersy628.coffeeorder.outbox.application;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxPublisherPropertiesTest {

	@Test
	void acceptsTheApprovedDefaultPolicy() {
		OutboxPublisherProperties properties = new OutboxPublisherProperties();

		assertThatCode(properties::validate).doesNotThrowAnyException();
		assertThat(properties.getMaxAttempts()).isEqualTo(6);
		assertThat(properties.getLeaseTimeout()).isGreaterThan(properties.getHttpCallTimeout());
	}

	@Test
	void rejectsAnyMaxAttemptsSettingOtherThanTheApprovedSixAttempts() {
		OutboxPublisherProperties properties = new OutboxPublisherProperties();
		properties.setMaxAttempts(5);

		assertThatThrownBy(properties::validate)
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("fixed at 6");

		properties.setMaxAttempts(7);

		assertThatThrownBy(properties::validate)
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("fixed at 6");
	}

	@Test
	void rejectsAnEnabledPublisherWithoutAValidHttpBaseUrl() {
		OutboxPublisherProperties properties = new OutboxPublisherProperties();
		properties.setEnabled(true);
		properties.setDataPlatformBaseUrl("ftp://data-platform.example");

		assertThatThrownBy(properties::validate)
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("absolute HTTP(S) URL");
	}

	@Test
	void rejectsALeaseThatCannotOutliveTheWholeHttpDeadline() {
		OutboxPublisherProperties properties = new OutboxPublisherProperties();
		properties.setLeaseTimeout(Duration.ofSeconds(5));

		assertThatThrownBy(properties::validate)
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("longer than the HTTP call timeout");
	}
}
