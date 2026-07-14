package com.usersy628.coffeeorder.point.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.usersy628.coffeeorder.global.error.DomainException;
import com.usersy628.coffeeorder.global.error.ErrorCode;
import com.usersy628.coffeeorder.point.domain.PointHistory;
import com.usersy628.coffeeorder.point.domain.PointWallet;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PointChargeTransactionExecutor {

	static final String CHARGE_IDEMPOTENCY_CONSTRAINT = "uk_point_history_user_idempotency_key";

	private final PointWalletRepository pointWalletRepository;
	private final PointHistoryRepository pointHistoryRepository;
	private final Clock clock;

	public PointChargeTransactionExecutor(
		PointWalletRepository pointWalletRepository,
		PointHistoryRepository pointHistoryRepository,
		Clock clock
	) {
		this.pointWalletRepository = pointWalletRepository;
		this.pointHistoryRepository = pointHistoryRepository;
		this.clock = clock;
	}

	@Transactional(timeout = 5, propagation = Propagation.REQUIRES_NEW)
	public PointChargeResult execute(PointChargeCommand command, String requestHash) {
		PointWallet wallet = pointWalletRepository.findByUserIdForUpdate(command.userId())
			.orElseThrow(() -> new DomainException(ErrorCode.USER_NOT_FOUND));

		PointHistory existing = pointHistoryRepository
			.findChargeByUserIdAndIdempotencyKeyForUpdate(command.userId(), command.idempotencyKey())
			.orElse(null);
		if (existing != null) {
			if (!existing.getRequestHash().equals(requestHash)) {
				throw new DomainException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
			}
			return PointChargeResult.from(existing, true);
		}

		Instant chargedAt = clock.instant().truncatedTo(ChronoUnit.MICROS);
		long balance = wallet.charge(command.amount(), chargedAt);
		PointHistory history = PointHistory.charge(
			command.userId(),
			command.amount(),
			balance,
			command.idempotencyKey(),
			requestHash,
			chargedAt
		);

		try {
			pointHistoryRepository.saveAndFlush(history);
		} catch (DataIntegrityViolationException exception) {
			if (containsConstraintName(exception, CHARGE_IDEMPOTENCY_CONSTRAINT)) {
				throw new ChargeIdempotencyConflictException(exception);
			}
			throw exception;
		}
		return PointChargeResult.from(history, false);
	}

	private boolean containsConstraintName(Throwable throwable, String constraintName) {
		Throwable current = throwable;
		while (current != null) {
			if (current.getMessage() != null && current.getMessage().contains(constraintName)) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	static final class ChargeIdempotencyConflictException extends RuntimeException {

		ChargeIdempotencyConflictException(Throwable cause) {
			super(cause);
		}
	}
}
