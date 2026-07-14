package com.usersy628.coffeeorder.outbox.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

final class OutboxDeadlineExecutor extends ThreadPoolExecutor implements AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(OutboxDeadlineExecutor.class);
	private static final long TERMINATION_WAIT_SECONDS = 5L;

	OutboxDeadlineExecutor(int maxConcurrency, ThreadFactory threadFactory) {
		super(
			maxConcurrency,
			maxConcurrency,
			0L,
			TimeUnit.MILLISECONDS,
			new SynchronousQueue<>(),
			threadFactory,
			new AbortPolicy()
		);
	}

	@Override
	public void close() {
		shutdownNow();
		try {
			if (!awaitTermination(TERMINATION_WAIT_SECONDS, TimeUnit.SECONDS)) {
				log.warn("Outbox HTTP deadline executor did not terminate within {} seconds", TERMINATION_WAIT_SECONDS);
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			log.warn("Interrupted while waiting for the outbox HTTP deadline executor to terminate", exception);
		}
	}
}
