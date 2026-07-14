package com.usersy628.coffeeorder.outbox.application;

import com.usersy628.coffeeorder.outbox.domain.ClaimedOutboxEvent;
import com.usersy628.coffeeorder.outbox.domain.DeliveryResult;

public interface DataPlatformClient {

	DeliveryResult send(ClaimedOutboxEvent event);
}
