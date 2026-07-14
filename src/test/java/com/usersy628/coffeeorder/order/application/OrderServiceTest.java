package com.usersy628.coffeeorder.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.usersy628.coffeeorder.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.PessimisticLockingFailureException;

class OrderServiceTest {

	private OrderRequestHasher requestHasher;
	private OrderTransactionExecutor transactionExecutor;
	private OrderService orderService;

	@BeforeEach
	void setUp() {
		requestHasher = mock(OrderRequestHasher.class);
		transactionExecutor = mock(OrderTransactionExecutor.class);
		OrderRetryProperties properties = new OrderRetryProperties();
		properties.setMaxAttempts(3);
		properties.setInitialBackoff(Duration.ZERO);
		properties.setJitterFactor(0.0);
		orderService = new OrderService(requestHasher, transactionExecutor, properties);
	}

	@Test
	void retriesTheWholeCommandInANewTransactionAfterTransientLockFailures() {
		OrderCommand command = command("retry-key");
		OrderResult success = result(false);
		when(requestHasher.hash(command)).thenReturn("hash");
		when(transactionExecutor.execute(command, "hash"))
			.thenThrow(new PessimisticLockingFailureException("forced deadlock"))
			.thenThrow(new PessimisticLockingFailureException("forced lock timeout"))
			.thenReturn(success);

		assertThat(orderService.create(command)).isEqualTo(success);
		verify(transactionExecutor, times(3)).execute(command, "hash");
	}

	@Test
	void returnsConcurrentRequestTimeoutAfterRetryExhaustion() {
		OrderCommand command = command("exhausted-key");
		PessimisticLockingFailureException failure =
			new PessimisticLockingFailureException("forced lock timeout");
		when(requestHasher.hash(command)).thenReturn("hash");
		when(transactionExecutor.execute(command, "hash")).thenThrow(failure);

		assertThatThrownBy(() -> orderService.create(command))
			.isInstanceOfSatisfying(OrderRetryFailureException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONCURRENT_REQUEST_TIMEOUT);
				assertThat(exception.getAttemptCount()).isEqualTo(3);
				assertThat(exception.getCause()).isSameAs(failure);
			});
		verify(transactionExecutor, times(3)).execute(command, "hash");
	}

	private OrderCommand command(String key) {
		return new OrderCommand(1L, List.of(new OrderCommand.Item(1L, 1)), key);
	}

	private OrderResult result(boolean replayed) {
		return new OrderResult(
			1L, 1L, "PAID", 4500L, 25500L,
			List.of(new OrderResult.Item(1L, "아메리카노", 4500L, 1, 4500L)),
			Instant.EPOCH, replayed
		);
	}
}
