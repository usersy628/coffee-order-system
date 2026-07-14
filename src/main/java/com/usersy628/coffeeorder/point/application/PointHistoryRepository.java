package com.usersy628.coffeeorder.point.application;

import java.util.Optional;

import com.usersy628.coffeeorder.point.domain.PointHistory;

public interface PointHistoryRepository {

	Optional<PointHistory> findChargeByUserIdAndIdempotencyKey(long userId, String idempotencyKey);

	Optional<PointHistory> findChargeByUserIdAndIdempotencyKeyForUpdate(long userId, String idempotencyKey);

	PointHistory saveAndFlush(PointHistory pointHistory);
}
