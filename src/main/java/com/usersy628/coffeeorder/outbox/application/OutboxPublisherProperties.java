package com.usersy628.coffeeorder.outbox.application;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;

@Component
@ConfigurationProperties("coffee-order.outbox.publisher")
public class OutboxPublisherProperties {

	private boolean enabled;
	private Duration pollInterval = Duration.ofSeconds(1);
	private int batchSize = 10;
	private int maxConcurrency = 10;
	private Duration httpCallTimeout = Duration.ofSeconds(5);
	private Duration leaseTimeout = Duration.ofSeconds(30);
	private int maxAttempts = 6;
	private Duration initialBackoff = Duration.ofSeconds(1);
	private double jitterFactor = 0.2;
	private String dataPlatformBaseUrl = "";

	@PostConstruct
	void validate() {
		if (batchSize < 1 || maxConcurrency < 1 || maxAttempts < 1 || maxAttempts > 6) {
			throw new IllegalStateException("Outbox batch size and concurrency must be positive, and max attempts must be between 1 and 6");
		}
		if (!isPositive(pollInterval)
			|| !isPositive(httpCallTimeout)
			|| !isPositive(leaseTimeout)
			|| !isPositive(initialBackoff)) {
			throw new IllegalStateException("Outbox duration properties must be positive");
		}
		if (leaseTimeout.compareTo(httpCallTimeout) <= 0) {
			throw new IllegalStateException("Outbox lease timeout must be longer than the HTTP call timeout");
		}
		if (jitterFactor < 0.0 || jitterFactor > 1.0) {
			throw new IllegalStateException("Outbox jitter factor must be between 0 and 1");
		}
		if (enabled) {
			validateDataPlatformBaseUrl();
		}
	}

	private boolean isPositive(Duration duration) {
		return duration != null && !duration.isNegative() && !duration.isZero();
	}

	private void validateDataPlatformBaseUrl() {
		if (dataPlatformBaseUrl.isBlank()) {
			throw new IllegalStateException("An enabled outbox publisher requires a data platform base URL");
		}
		try {
			URI uri = URI.create(dataPlatformBaseUrl);
			String scheme = uri.getScheme();
			if (uri.getHost() == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
				throw new IllegalStateException("Outbox data platform base URL must be an absolute HTTP(S) URL");
			}
		} catch (IllegalArgumentException exception) {
			throw new IllegalStateException("Outbox data platform base URL must be an absolute HTTP(S) URL", exception);
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Duration getPollInterval() {
		return pollInterval;
	}

	public void setPollInterval(Duration pollInterval) {
		this.pollInterval = pollInterval;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public int getMaxConcurrency() {
		return maxConcurrency;
	}

	public void setMaxConcurrency(int maxConcurrency) {
		this.maxConcurrency = maxConcurrency;
	}

	public Duration getHttpCallTimeout() {
		return httpCallTimeout;
	}

	public void setHttpCallTimeout(Duration httpCallTimeout) {
		this.httpCallTimeout = httpCallTimeout;
	}

	public Duration getLeaseTimeout() {
		return leaseTimeout;
	}

	public void setLeaseTimeout(Duration leaseTimeout) {
		this.leaseTimeout = leaseTimeout;
	}

	public int getMaxAttempts() {
		return maxAttempts;
	}

	public void setMaxAttempts(int maxAttempts) {
		this.maxAttempts = maxAttempts;
	}

	public Duration getInitialBackoff() {
		return initialBackoff;
	}

	public void setInitialBackoff(Duration initialBackoff) {
		this.initialBackoff = initialBackoff;
	}

	public double getJitterFactor() {
		return jitterFactor;
	}

	public void setJitterFactor(double jitterFactor) {
		this.jitterFactor = jitterFactor;
	}

	public String getDataPlatformBaseUrl() {
		return dataPlatformBaseUrl;
	}

	public void setDataPlatformBaseUrl(String dataPlatformBaseUrl) {
		this.dataPlatformBaseUrl = dataPlatformBaseUrl == null ? "" : dataPlatformBaseUrl.trim();
	}
}
