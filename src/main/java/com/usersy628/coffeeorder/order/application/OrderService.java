package com.usersy628.coffeeorder.order.application;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

	private final OrderRequestHasher requestHasher;
	private final OrderTransactionExecutor transactionExecutor;
	private final OrderRetryProperties retryProperties;

	public OrderService(
		OrderRequestHasher requestHasher,
		OrderTransactionExecutor transactionExecutor,
		OrderRetryProperties retryProperties
	) {
		this.requestHasher = requestHasher;
		this.transactionExecutor = transactionExecutor;
		this.retryProperties = retryProperties;
	}

	public OrderResult create(OrderCommand command) {
		String requestHash = requestHasher.hash(command);
		for (int attempt = 1; attempt <= retryProperties.getMaxAttempts(); attempt++) {
			try {
				return transactionExecutor.execute(command, requestHash);
			} catch (PessimisticLockingFailureException exception) {
				if (attempt == retryProperties.getMaxAttempts()) {
					throw new OrderRetryFailureException(attempt, exception);
				}
				backoff(attempt);
			}
		}
		throw new IllegalStateException("Order retry loop completed unexpectedly");
	}

	private void backoff(int failedAttempt) {
		long baseMillis = retryProperties.getInitialBackoff().toMillis() * (1L << (failedAttempt - 1));
		double jitter = retryProperties.getJitterFactor();
		long minMillis = Math.max(0L, Math.round(baseMillis * (1.0 - jitter)));
		long maxMillis = Math.max(minMillis, Math.round(baseMillis * (1.0 + jitter)));
		long delayMillis = ThreadLocalRandom.current().nextLong(minMillis, maxMillis + 1);
		try {
			Thread.sleep(delayMillis);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new OrderRetryFailureException(failedAttempt, exception);
		}
	}
}
