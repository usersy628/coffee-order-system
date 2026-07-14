package com.usersy628.coffeeorder.order.application;

import java.util.Objects;

import com.usersy628.coffeeorder.global.error.DomainException;
import com.usersy628.coffeeorder.global.error.ErrorCode;

public class OrderRetryFailureException extends DomainException {

	private final int attemptCount;

	public OrderRetryFailureException(int attemptCount, Throwable cause) {
		super(ErrorCode.CONCURRENT_REQUEST_TIMEOUT);
		if (attemptCount < 1) {
			throw new IllegalArgumentException("attemptCount must be at least 1");
		}
		this.attemptCount = attemptCount;
		initCause(Objects.requireNonNull(cause, "cause must not be null"));
	}

	public int getAttemptCount() {
		return attemptCount;
	}
}
