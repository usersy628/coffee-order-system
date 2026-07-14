package com.usersy628.coffeeorder.outbox.application;

import com.usersy628.coffeeorder.outbox.domain.ClaimedOutboxEvent;
import com.usersy628.coffeeorder.outbox.domain.DeliveryResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

@Service
public class OutboxPublisher {

	private final OutboxPublisherProperties properties;
	private final OutboxClaimTransactionExecutor claimTransactionExecutor;
	private final OutboxStateTransactionExecutor stateTransactionExecutor;
	private final OutboxRetryPolicy retryPolicy;
	private final DataPlatformClient dataPlatformClient;
	private final ExecutorService workerExecutor;
	private final Semaphore workerSlots;

	public OutboxPublisher(
		OutboxPublisherProperties properties,
		OutboxClaimTransactionExecutor claimTransactionExecutor,
		OutboxStateTransactionExecutor stateTransactionExecutor,
		OutboxRetryPolicy retryPolicy,
		DataPlatformClient dataPlatformClient,
		@Qualifier("outboxPublisherExecutor") ExecutorService workerExecutor
	) {
		this.properties = properties;
		this.claimTransactionExecutor = claimTransactionExecutor;
		this.stateTransactionExecutor = stateTransactionExecutor;
		this.retryPolicy = retryPolicy;
		this.dataPlatformClient = dataPlatformClient;
		this.workerExecutor = workerExecutor;
		this.workerSlots = new Semaphore(properties.getMaxConcurrency());
	}

	public synchronized List<CompletableFuture<Void>> publishDueEvents() {
		int claimLimit = Math.min(properties.getBatchSize(), workerSlots.availablePermits());
		if (claimLimit < 1 || !workerSlots.tryAcquire(claimLimit)) {
			return List.of();
		}

		List<ClaimedOutboxEvent> claimedEvents;
		try {
			claimedEvents = claimTransactionExecutor.reclaimAndClaim(claimLimit);
		} catch (RuntimeException exception) {
			workerSlots.release(claimLimit);
			throw exception;
		}
		workerSlots.release(claimLimit - claimedEvents.size());

		List<CompletableFuture<Void>> futures = new ArrayList<>();
		for (ClaimedOutboxEvent event : claimedEvents) {
			try {
				futures.add(CompletableFuture.runAsync(() -> {
					try {
						deliver(event);
					} finally {
						workerSlots.release();
					}
				}, workerExecutor));
			} catch (RuntimeException exception) {
				workerSlots.release();
				throw exception;
			}
		}
		return futures;
	}

	private void deliver(ClaimedOutboxEvent event) {
		DeliveryResult result;
		try {
			result = Objects.requireNonNull(dataPlatformClient.send(event), "delivery result must not be null");
		} catch (RuntimeException exception) {
			result = DeliveryResult.retryableFailure("HTTP_CLIENT_" + exception.getClass().getSimpleName());
		}

		switch (result.outcome()) {
			case PUBLISHED -> stateTransactionExecutor.markPublished(event);
			case PERMANENT_FAILURE -> stateTransactionExecutor.markFailed(event, result.errorSummary());
			case RETRYABLE_FAILURE -> scheduleRetryOrFail(event, result.errorSummary());
		}
	}

	private void scheduleRetryOrFail(ClaimedOutboxEvent event, String errorSummary) {
		if (event.attemptCount() >= properties.getMaxAttempts()) {
			stateTransactionExecutor.markFailed(event, errorSummary);
			return;
		}
		stateTransactionExecutor.scheduleRetry(event, retryPolicy.nextRetryDelay(event.attemptCount()), errorSummary);
	}
}
