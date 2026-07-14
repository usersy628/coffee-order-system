package com.usersy628.coffeeorder.point.application;

import java.util.Optional;

import com.usersy628.coffeeorder.global.error.DomainException;
import com.usersy628.coffeeorder.global.error.ErrorCode;
import com.usersy628.coffeeorder.point.domain.PointHistory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PointChargeReplayReader {

	private final PointHistoryRepository pointHistoryRepository;

	public PointChargeReplayReader(PointHistoryRepository pointHistoryRepository) {
		this.pointHistoryRepository = pointHistoryRepository;
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
	public Optional<PointChargeResult> findExisting(PointChargeCommand command, String requestHash) {
		return pointHistoryRepository
			.findChargeByUserIdAndIdempotencyKey(command.userId(), command.idempotencyKey())
			.map(history -> replay(history, requestHash));
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
	public PointChargeResult readAfterUniqueConflict(PointChargeCommand command, String requestHash) {
		PointHistory history = pointHistoryRepository
			.findChargeByUserIdAndIdempotencyKey(command.userId(), command.idempotencyKey())
			.orElseThrow(() -> new IllegalStateException("Committed charge history was not found after unique conflict"));
		return replay(history, requestHash);
	}

	private PointChargeResult replay(PointHistory history, String requestHash) {
		if (!history.getRequestHash().equals(requestHash)) {
			throw new DomainException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
		}
		return PointChargeResult.from(history, true);
	}
}
