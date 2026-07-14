package com.usersy628.coffeeorder.point.application;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class PointChargeService {

	private final PointChargeRequestHasher requestHasher;
	private final PointChargeReplayReader replayReader;
	private final PointChargeTransactionExecutor transactionExecutor;
	private final PointChargeRetryProperties retryProperties;

	public PointChargeService(
		PointChargeRequestHasher requestHasher,
		PointChargeReplayReader replayReader,
		PointChargeTransactionExecutor transactionExecutor,
		PointChargeRetryProperties retryProperties
	) {
		this.requestHasher = requestHasher;
		this.replayReader = replayReader;
		this.transactionExecutor = transactionExecutor;
		this.retryProperties = retryProperties;
	}

	public PointChargeResult charge(PointChargeCommand command) {
		String requestHash = requestHasher.hash(command.amount());
		Optional<PointChargeResult> existing = replayReader.findExisting(command, requestHash);
		if (existing.isPresent()) {
			return existing.get();
		}

		for (int attempt = 1; attempt <= retryProperties.getMaxAttempts(); attempt++) {
			try {
				return transactionExecutor.execute(command, requestHash);
			} catch (PointChargeTransactionExecutor.ChargeIdempotencyConflictException exception) {
				return replayReader.readAfterUniqueConflict(command, requestHash);
			} catch (PessimisticLockingFailureException exception) {
				if (attempt == retryProperties.getMaxAttempts()) {
					throw new PointChargeRetryFailureException(attempt, exception);
				}
				backoff(attempt);
			}
		}
		throw new IllegalStateException("Point charge retry loop completed unexpectedly");
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
			throw new PointChargeRetryFailureException(failedAttempt, exception);
		}
	}
}
