package com.usersy628.coffeeorder.point.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.usersy628.coffeeorder.global.error.DomainException;
import com.usersy628.coffeeorder.global.error.ErrorCode;
import com.usersy628.coffeeorder.point.application.PointChargeCommand;
import com.usersy628.coffeeorder.point.application.PointChargeRequestHasher;
import com.usersy628.coffeeorder.point.application.PointChargeResult;
import com.usersy628.coffeeorder.point.application.PointChargeService;
import com.usersy628.coffeeorder.point.application.PointChargeTransactionExecutor;
import com.usersy628.coffeeorder.support.testcontainers.MySqlIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@MySqlIntegrationTest
class PointChargeConcurrencyIntegrationTest {

	@Autowired
	private PointChargeService pointChargeService;

	@Autowired
	private PointChargeTransactionExecutor transactionExecutor;

	@Autowired
	private PointChargeRequestHasher requestHasher;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DataSource dataSource;

	private ExecutorService executorService;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM point_history");
		jdbcTemplate.update("UPDATE point_wallet SET balance = 0, updated_at = UTC_TIMESTAMP(6)");
		executorService = Executors.newFixedThreadPool(20);
	}

	@AfterEach
	void tearDown() {
		executorService.shutdownNow();
	}

	@Test
	void serializesOneHundredChargesForTheSameUserWithoutLostUpdates() throws Exception {
		List<PointChargeResult> results = runConcurrently(100, index ->
			new PointChargeCommand(1L, 1L, "same-user-" + index));

		assertThat(results).hasSize(100);
		assertThat(walletBalance(1L)).isEqualTo(100L);
		assertThat(historyCount(1L)).isEqualTo(100);
	}

	@Test
	void appliesOneConcurrentChargeForOneIdempotencyKey() throws Exception {
		List<PointChargeResult> results = runConcurrently(100, index ->
			new PointChargeCommand(1L, 10L, "same-idempotency-key"));

		assertThat(results).hasSize(100);
		assertThat(results.stream().filter(result -> !result.replayed()).count()).isEqualTo(1);
		assertThat(results.stream().filter(PointChargeResult::replayed).count()).isEqualTo(99);
		assertThat(walletBalance(1L)).isEqualTo(10L);
		assertThat(historyCount(1L)).isEqualTo(1);
	}

	@Test
	void processesDifferentUsersInParallelWithoutSharingWalletLocks() throws Exception {
		List<PointChargeResult> results = runConcurrently(100, index -> {
			long userId = index % 2 == 0 ? 1L : 2L;
			return new PointChargeCommand(userId, 1L, "parallel-user-" + index);
		});

		assertThat(results).hasSize(100);
		assertThat(walletBalance(1L)).isEqualTo(50L);
		assertThat(walletBalance(2L)).isEqualTo(50L);
		assertThat(historyCount(1L)).isEqualTo(50);
		assertThat(historyCount(2L)).isEqualTo(50);
	}

	@Test
	void returnsConcurrentRequestTimeoutAfterRealMySqlLockWaitRetriesAreExhausted() throws Exception {
		try (Connection lockConnection = dataSource.getConnection()) {
			lockConnection.setAutoCommit(false);
			try (PreparedStatement statement = lockConnection.prepareStatement(
				"SELECT user_id FROM point_wallet WHERE user_id = 1 FOR UPDATE"
			)) {
				statement.executeQuery();

				assertThatThrownBy(() -> pointChargeService.charge(
					new PointChargeCommand(1L, 100L, "lock-timeout-key")
				))
					.isInstanceOfSatisfying(DomainException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONCURRENT_REQUEST_TIMEOUT));
			} finally {
				lockConnection.rollback();
			}
		}

		assertThat(walletBalance(1L)).isZero();
		assertThat(historyCount(1L)).isZero();
	}

	@Test
	void rollsBackTheWalletWhenHistoryPersistenceFails() {
		String tooLongIdempotencyKey = "k".repeat(256);
		PointChargeCommand command = new PointChargeCommand(1L, 100L, tooLongIdempotencyKey);

		assertThatThrownBy(() -> transactionExecutor.execute(command, requestHasher.hash(command.amount())))
			.isInstanceOf(DataIntegrityViolationException.class);

		assertThat(walletBalance(1L)).isZero();
		assertThat(historyCount(1L)).isZero();
	}

	@Test
	void translatesAForcedRealMySqlDeadlockAsARetryableLockFailure() throws Exception {
		CyclicBarrier bothFirstRowsLocked = new CyclicBarrier(2);
		Future<Throwable> first = executorService.submit(() ->
			runOppositeWalletLockOrder(1L, 2L, bothFirstRowsLocked));
		Future<Throwable> second = executorService.submit(() ->
			runOppositeWalletLockOrder(2L, 1L, bothFirstRowsLocked));

		List<Throwable> failures = java.util.stream.Stream.of(
			first.get(10, TimeUnit.SECONDS),
			second.get(10, TimeUnit.SECONDS)
		).filter(java.util.Objects::nonNull).toList();

		assertThat(failures).hasSize(1);
		assertThat(failures.get(0)).isInstanceOf(PessimisticLockingFailureException.class);
	}

	private List<PointChargeResult> runConcurrently(
		int requestCount,
		java.util.function.IntFunction<PointChargeCommand> commandFactory
	) throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		List<Future<PointChargeResult>> futures = new ArrayList<>();
		for (int index = 0; index < requestCount; index++) {
			int requestIndex = index;
			futures.add(executorService.submit(() -> {
				start.await();
				return pointChargeService.charge(commandFactory.apply(requestIndex));
			}));
		}
		start.countDown();

		List<PointChargeResult> results = new ArrayList<>();
		for (Future<PointChargeResult> future : futures) {
			results.add(future.get());
		}
		return results;
	}

	private Throwable runOppositeWalletLockOrder(
		long firstUserId,
		long secondUserId,
		CyclicBarrier bothFirstRowsLocked
	) {
		TransactionTemplate transactionTemplate = new TransactionTemplate(
			new DataSourceTransactionManager(dataSource)
		);
		try {
			transactionTemplate.executeWithoutResult(status -> {
				lockWallet(firstUserId);
				try {
					bothFirstRowsLocked.await(5, TimeUnit.SECONDS);
				} catch (Exception exception) {
					throw new IllegalStateException("Failed to coordinate the forced deadlock", exception);
				}
				lockWallet(secondUserId);
			});
			return null;
		} catch (Throwable throwable) {
			return throwable;
		}
	}

	private void lockWallet(long userId) {
		jdbcTemplate.queryForObject(
			"SELECT user_id FROM point_wallet WHERE user_id = ? FOR UPDATE",
			Long.class,
			userId
		);
	}

	private long walletBalance(long userId) {
		return jdbcTemplate.queryForObject(
			"SELECT balance FROM point_wallet WHERE user_id = ?",
			Long.class,
			userId
		);
	}

	private int historyCount(long userId) {
		return jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM point_history WHERE user_id = ?",
			Integer.class,
			userId
		);
	}
}
