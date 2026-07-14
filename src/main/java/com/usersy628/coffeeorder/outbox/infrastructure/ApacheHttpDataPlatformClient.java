package com.usersy628.coffeeorder.outbox.infrastructure;

import com.usersy628.coffeeorder.outbox.application.DataPlatformClient;
import com.usersy628.coffeeorder.outbox.application.OutboxPublisherProperties;
import com.usersy628.coffeeorder.outbox.domain.ClaimedOutboxEvent;
import com.usersy628.coffeeorder.outbox.domain.DeliveryResult;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class ApacheHttpDataPlatformClient implements DataPlatformClient {

	private static final String EVENT_PATH = "/internal/mock-data-platform/events";

	private final CloseableHttpClient httpClient;
	private final OutboxPublisherProperties properties;
	private final ExecutorService deadlineExecutor;

	public ApacheHttpDataPlatformClient(
		CloseableHttpClient outboxHttpClient,
		OutboxPublisherProperties properties,
		@Qualifier("outboxDeadlineExecutor") ExecutorService deadlineExecutor
	) {
		this.httpClient = outboxHttpClient;
		this.properties = properties;
		this.deadlineExecutor = deadlineExecutor;
	}

	@Override
	public DeliveryResult send(ClaimedOutboxEvent event) {
		if (properties.getDataPlatformBaseUrl().isBlank()) {
			return DeliveryResult.retryableFailure("DATA_PLATFORM_BASE_URL_MISSING");
		}
		HttpPost request = requestFor(event);
		Future<DeliveryResult> delivery;
		try {
			delivery = deadlineExecutor.submit(() -> sendOnce(request));
		} catch (RejectedExecutionException exception) {
			return DeliveryResult.retryableFailure("HTTP_DEADLINE_CAPACITY_EXHAUSTED");
		}
		try {
			long timeoutMillis = Math.max(1L, properties.getHttpCallTimeout().toMillis());
			return delivery.get(timeoutMillis, TimeUnit.MILLISECONDS);
		} catch (TimeoutException exception) {
			request.cancel();
			delivery.cancel(true);
			return DeliveryResult.retryableFailure("HTTP_TIMEOUT");
		} catch (InterruptedException exception) {
			request.cancel();
			delivery.cancel(true);
			Thread.currentThread().interrupt();
			return DeliveryResult.retryableFailure("HTTP_INTERRUPTED");
		} catch (ExecutionException exception) {
			return DeliveryResult.retryableFailure("HTTP_NETWORK_ERROR");
		}
	}

	private HttpPost requestFor(ClaimedOutboxEvent event) {
		HttpPost request = new HttpPost(endpointUri());
		request.setHeader("Idempotency-Key", event.eventId());
		request.setEntity(new StringEntity(event.payload(), ContentType.APPLICATION_JSON));
		return request;
	}

	private DeliveryResult sendOnce(HttpPost request) throws IOException {
		return httpClient.execute(request, response -> {
			EntityUtils.consume(response.getEntity());
			return classify(response.getCode());
		});
	}

	private URI endpointUri() {
		String baseUrl = properties.getDataPlatformBaseUrl();
		String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		return URI.create(normalizedBaseUrl + EVENT_PATH);
	}

	private DeliveryResult classify(int statusCode) {
		if (statusCode >= 200 && statusCode < 300) {
			return DeliveryResult.published();
		}
		if (statusCode >= 400 && statusCode < 500) {
			return DeliveryResult.permanentFailure("HTTP_" + statusCode);
		}
		return DeliveryResult.retryableFailure("HTTP_" + statusCode);
	}
}
