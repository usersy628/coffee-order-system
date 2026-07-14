package com.usersy628.coffeeorder.point.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import com.usersy628.coffeeorder.global.error.DomainException;
import com.usersy628.coffeeorder.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.PessimisticLockingFailureException;

class PointChargeServiceTest {

	private PointChargeRequestHasher requestHasher;
	private PointChargeReplayReader replayReader;
	private PointChargeTransactionExecutor transactionExecutor;
	private PointChargeService pointChargeService;

	@BeforeEach
	void setUp() {
		requestHasher = mock(PointChargeRequestHasher.class);
		replayReader = mock(PointChargeReplayReader.class);
		transactionExecutor = mock(PointChargeTransactionExecutor.class);
		PointChargeRetryProperties properties = new PointChargeRetryProperties();
		properties.setMaxAttempts(3);
		properties.setInitialBackoff(Duration.ZERO);
		properties.setJitterFactor(0.0);
		pointChargeService = new PointChargeService(
			requestHasher,
			replayReader,
			transactionExecutor,
			properties
		);
	}

	@Test
	void retriesTheWholeCommandInANewCallAfterTransientLockFailures() {
		PointChargeCommand command = new PointChargeCommand(1L, 100L, "retry-key");
		PointChargeResult success = new PointChargeResult(1L, 100L, 100L, Instant.EPOCH, false);
		when(requestHasher.hash(100L)).thenReturn("hash");
		when(replayReader.findExisting(command, "hash")).thenReturn(Optional.empty());
		when(transactionExecutor.execute(command, "hash"))
			.thenThrow(new PessimisticLockingFailureException("forced deadlock"))
			.thenThrow(new PessimisticLockingFailureException("forced lock timeout"))
			.thenReturn(success);

		PointChargeResult result = pointChargeService.charge(command);

		assertThat(result).isEqualTo(success);
		verify(transactionExecutor, org.mockito.Mockito.times(3)).execute(command, "hash");
	}

	@Test
	void returnsConcurrentRequestTimeoutAfterRetryExhaustion() {
		PointChargeCommand command = new PointChargeCommand(1L, 100L, "exhausted-key");
		when(requestHasher.hash(100L)).thenReturn("hash");
		when(replayReader.findExisting(command, "hash")).thenReturn(Optional.empty());
		when(transactionExecutor.execute(command, "hash"))
			.thenThrow(new PessimisticLockingFailureException("forced lock timeout"));

		assertThatThrownBy(() -> pointChargeService.charge(command))
			.isInstanceOfSatisfying(DomainException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONCURRENT_REQUEST_TIMEOUT));
		verify(transactionExecutor, org.mockito.Mockito.times(3)).execute(command, "hash");
	}
}
