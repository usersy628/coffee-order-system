package com.usersy628.coffeeorder.point.application;

import java.time.Instant;

import com.usersy628.coffeeorder.point.domain.PointHistory;

public record PointChargeResult(
	long userId,
	long chargedAmount,
	long balance,
	Instant chargedAt,
	boolean replayed
) {

	public static PointChargeResult from(PointHistory history, boolean replayed) {
		return new PointChargeResult(
			history.getUserId(),
			history.getAmount(),
			history.getBalanceAfter(),
			history.getCreatedAt(),
			replayed
		);
	}
}
