package com.usersy628.coffeeorder.outbox.infrastructure;

import com.usersy628.coffeeorder.outbox.application.OutboxPublisherProperties;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class OutboxHttpConfiguration {

	@Bean(destroyMethod = "close")
	CloseableHttpClient outboxHttpClient(OutboxPublisherProperties properties) {
		Timeout timeout = Timeout.ofMilliseconds(properties.getHttpCallTimeout().toMillis());
		RequestConfig requestConfig = RequestConfig.custom()
			.setConnectionRequestTimeout(timeout)
			.setResponseTimeout(timeout)
			.build();
		return HttpClients.custom()
			.setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
				.setMaxConnTotal(properties.getMaxConcurrency())
				.setMaxConnPerRoute(properties.getMaxConcurrency())
				.setDefaultConnectionConfig(ConnectionConfig.custom()
					.setConnectTimeout(timeout)
					.setSocketTimeout(timeout)
					.build())
				.build())
			.disableAutomaticRetries()
			.disableRedirectHandling()
			.setDefaultRequestConfig(requestConfig)
			.build();
	}

	@Bean(name = "outboxPublisherExecutor", destroyMethod = "shutdown")
	ExecutorService outboxPublisherExecutor(OutboxPublisherProperties properties) {
		return Executors.newFixedThreadPool(
			properties.getMaxConcurrency(), namedThreadFactory("outbox-publisher-")
		);
	}

	@Bean(name = "outboxDeadlineExecutor", destroyMethod = "shutdown")
	ExecutorService outboxDeadlineExecutor(OutboxPublisherProperties properties) {
		int maxConcurrency = properties.getMaxConcurrency();
		return new ThreadPoolExecutor(
			maxConcurrency,
			maxConcurrency,
			0L,
			TimeUnit.MILLISECONDS,
			new SynchronousQueue<>(),
			namedThreadFactory("outbox-http-deadline-"),
			new ThreadPoolExecutor.AbortPolicy()
		);
	}

	private ThreadFactory namedThreadFactory(String prefix) {
		AtomicInteger sequence = new AtomicInteger();
		return runnable -> new Thread(runnable, prefix + sequence.incrementAndGet());
	}
}
