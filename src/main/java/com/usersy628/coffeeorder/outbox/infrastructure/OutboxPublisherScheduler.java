package com.usersy628.coffeeorder.outbox.infrastructure;

import com.usersy628.coffeeorder.outbox.application.OutboxPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "coffee-order.outbox.publisher", name = "enabled", havingValue = "true")
public class OutboxPublisherScheduler {

	private final OutboxPublisher outboxPublisher;

	public OutboxPublisherScheduler(OutboxPublisher outboxPublisher) {
		this.outboxPublisher = outboxPublisher;
	}

	@Scheduled(fixedDelayString = "${coffee-order.outbox.publisher.poll-interval}")
	public void publishDueEvents() {
		outboxPublisher.publishDueEvents();
	}
}
