package com.usersy628.coffeeorder.outbox.infrastructure;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.usersy628.coffeeorder.outbox.application.OutboxPublisherProperties;
import com.usersy628.coffeeorder.outbox.domain.ClaimedOutboxEvent;
import com.usersy628.coffeeorder.outbox.domain.DeliveryResult;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

class ApacheHttpDataPlatformClientWireMockTest {

	private static final String EVENT_PATH = "/internal/mock-data-platform/events";

	private WireMockServer wireMockServer;
	private CloseableHttpClient httpClient;
	private ExecutorService deadlineExecutor;

	@BeforeEach
	void startWireMock() {
		wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
		wireMockServer.start();
	}

	@AfterEach
	void stopResources() throws Exception {
		if (deadlineExecutor != null) {
			deadlineExecutor.shutdownNow();
		}
		if (httpClient != null) {
			httpClient.close();
		}
		if (wireMockServer != null) {
			wireMockServer.stop();
		}
	}

	@Test
	void sendsTheOriginalPayloadAndIdempotencyKeyExactlyOnce() {
		ClaimedOutboxEvent event = event();
		wireMockServer.stubFor(post(urlEqualTo(EVENT_PATH))
			.withHeader("Idempotency-Key", equalTo(event.eventId()))
			.withHeader("Content-Type", containing("application/json"))
			.withRequestBody(equalToJson(event.payload()))
			.willReturn(aResponse().withStatus(201)));

		DeliveryResult result = client(Duration.ofSeconds(1)).send(event);

		assertThat(result.outcome()).isEqualTo(DeliveryResult.Outcome.PUBLISHED);
		wireMockServer.verify(1, postRequestedFor(urlEqualTo(EVENT_PATH))
			.withHeader("Idempotency-Key", equalTo(event.eventId())));
	}

	@Test
	void classifies4xxAsPermanentAnd5xxAsRetryableWithoutAdapterRetries() {
		ClaimedOutboxEvent event = event();
		wireMockServer.stubFor(post(urlEqualTo(EVENT_PATH)).willReturn(aResponse().withStatus(400)));
		ApacheHttpDataPlatformClient client = client(Duration.ofSeconds(1));

		DeliveryResult permanent = client.send(event);

		assertThat(permanent.outcome()).isEqualTo(DeliveryResult.Outcome.PERMANENT_FAILURE);
		assertThat(permanent.errorSummary()).isEqualTo("HTTP_400");
		wireMockServer.verify(1, postRequestedFor(urlEqualTo(EVENT_PATH)));

		wireMockServer.resetAll();
		wireMockServer.stubFor(post(urlEqualTo(EVENT_PATH)).willReturn(aResponse().withStatus(503)));

		DeliveryResult retryable = client.send(event);

		assertThat(retryable.outcome()).isEqualTo(DeliveryResult.Outcome.RETRYABLE_FAILURE);
		assertThat(retryable.errorSummary()).isEqualTo("HTTP_503");
		wireMockServer.verify(1, postRequestedFor(urlEqualTo(EVENT_PATH)));
	}

	@Test
	void enforcesTheWholeHttpDeadlineForSlowResponses() throws Exception {
		ClaimedOutboxEvent event = event();
		wireMockServer.stubFor(post(urlEqualTo(EVENT_PATH))
			.willReturn(aResponse().withStatus(200).withFixedDelay(1_000)));

		long startedAt = System.nanoTime();
		DeliveryResult result = client(Duration.ofMillis(100)).send(event);
		Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

		assertThat(result.outcome()).isEqualTo(DeliveryResult.Outcome.RETRYABLE_FAILURE);
		assertThat(result.errorSummary()).isEqualTo("HTTP_TIMEOUT");
		assertThat(elapsed).isLessThan(Duration.ofMillis(800));
		assertThat(deadlineWorkerBecomesIdle()).isTrue();
	}

	@Test
	void classifiesConnectionResetAsRetryable() {
		ClaimedOutboxEvent event = event();
		wireMockServer.stubFor(post(urlEqualTo(EVENT_PATH))
			.willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

		DeliveryResult result = client(Duration.ofSeconds(1)).send(event);

		assertThat(result.outcome()).isEqualTo(DeliveryResult.Outcome.RETRYABLE_FAILURE);
		wireMockServer.verify(1, postRequestedFor(urlEqualTo(EVENT_PATH)));
	}

	@Test
	void rejectsImmediatelyInsteadOfQueuingWhenDeadlineWorkersAreExhausted() throws Exception {
		ClaimedOutboxEvent event = event();
		wireMockServer.stubFor(post(urlEqualTo(EVENT_PATH)).willReturn(aResponse().withStatus(201)));
		ApacheHttpDataPlatformClient client = client(Duration.ofSeconds(1));
		CountDownLatch workerStarted = new CountDownLatch(1);
		CountDownLatch releaseWorker = new CountDownLatch(1);
		deadlineExecutor.submit(() -> {
			workerStarted.countDown();
			try {
				releaseWorker.await();
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
			}
		});

		try {
			assertThat(workerStarted.await(2, TimeUnit.SECONDS)).isTrue();

			DeliveryResult result = client.send(event);

			assertThat(result.outcome()).isEqualTo(DeliveryResult.Outcome.RETRYABLE_FAILURE);
			assertThat(result.errorSummary()).isEqualTo("HTTP_DEADLINE_CAPACITY_EXHAUSTED");
			wireMockServer.verify(0, postRequestedFor(urlEqualTo(EVENT_PATH)));
		} finally {
			releaseWorker.countDown();
		}
	}

	@Test
	void closesDeadlineWorkersWithInterruptAndDoesNotLetThemBlockJvmShutdown() throws Exception {
		OutboxPublisherProperties properties = new OutboxPublisherProperties();
		properties.setMaxConcurrency(1);
		OutboxDeadlineExecutor executor = new OutboxHttpConfiguration().outboxDeadlineExecutor(properties);
		CountDownLatch workerStarted = new CountDownLatch(1);
		CountDownLatch workerInterrupted = new CountDownLatch(1);
		AtomicBoolean daemonWorker = new AtomicBoolean();
		try {
			executor.submit(() -> {
				daemonWorker.set(Thread.currentThread().isDaemon());
				workerStarted.countDown();
				try {
					new CountDownLatch(1).await();
				} catch (InterruptedException exception) {
					workerInterrupted.countDown();
					Thread.currentThread().interrupt();
				}
			});

			assertThat(workerStarted.await(2, TimeUnit.SECONDS)).isTrue();

			executor.close();

			assertThat(daemonWorker.get()).isTrue();
			assertThat(workerInterrupted.await(1, TimeUnit.SECONDS)).isTrue();
			assertThat(executor.isTerminated()).isTrue();
		} finally {
			executor.close();
		}
	}

	private ApacheHttpDataPlatformClient client(Duration timeout) {
		OutboxPublisherProperties properties = new OutboxPublisherProperties();
		properties.setDataPlatformBaseUrl(wireMockServer.baseUrl());
		properties.setHttpCallTimeout(timeout);
		properties.setMaxConcurrency(1);
		OutboxHttpConfiguration configuration = new OutboxHttpConfiguration();
		httpClient = configuration.outboxHttpClient(properties);
		deadlineExecutor = configuration.outboxDeadlineExecutor(properties);
		return new ApacheHttpDataPlatformClient(httpClient, properties, deadlineExecutor);
	}

	private boolean deadlineWorkerBecomesIdle() throws InterruptedException {
		ThreadPoolExecutor executor = (ThreadPoolExecutor) deadlineExecutor;
		for (int attempt = 0; attempt < 100; attempt++) {
			if (executor.getActiveCount() == 0) {
				return true;
			}
			Thread.sleep(10);
		}
		return false;
	}

	private ClaimedOutboxEvent event() {
		String eventId = UUID.randomUUID().toString();
		String payload = """
			{
			  "eventId": "%s",
			  "eventType": "ORDER_COMPLETED",
			  "schemaVersion": 1,
			  "occurredAt": "2026-07-15T00:00:00Z",
			  "orderId": 101,
			  "data": {"userId": 1, "totalAmount": 4500, "items": []}
			}
			""".formatted(eventId);
		return new ClaimedOutboxEvent(1L, eventId, payload, 1, UUID.randomUUID().toString());
	}
}
