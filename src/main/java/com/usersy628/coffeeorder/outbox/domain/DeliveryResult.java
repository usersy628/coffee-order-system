package com.usersy628.coffeeorder.outbox.domain;

import java.util.Objects;

public record DeliveryResult(Outcome outcome, String errorSummary) {

	public enum Outcome {
		PUBLISHED,
		RETRYABLE_FAILURE,
		PERMANENT_FAILURE
	}

	public DeliveryResult {
		Objects.requireNonNull(outcome, "outcome must not be null");
		if (outcome == Outcome.PUBLISHED && errorSummary != null) {
			throw new IllegalArgumentException("Published deliveries must not have an error summary");
		}
		if (outcome != Outcome.PUBLISHED && (errorSummary == null || errorSummary.isBlank())) {
			throw new IllegalArgumentException("Failed deliveries need an error summary");
		}
	}

	public static DeliveryResult published() {
		return new DeliveryResult(Outcome.PUBLISHED, null);
	}

	public static DeliveryResult retryableFailure(String errorSummary) {
		return new DeliveryResult(Outcome.RETRYABLE_FAILURE, errorSummary);
	}

	public static DeliveryResult permanentFailure(String errorSummary) {
		return new DeliveryResult(Outcome.PERMANENT_FAILURE, errorSummary);
	}
}
