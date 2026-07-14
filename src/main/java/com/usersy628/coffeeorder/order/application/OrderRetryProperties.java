package com.usersy628.coffeeorder.order.application;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("coffee-order.order.create.retry")
public class OrderRetryProperties {

	private int maxAttempts = 3;
	private Duration initialBackoff = Duration.ofMillis(50);
	private double jitterFactor = 0.2;

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
}
