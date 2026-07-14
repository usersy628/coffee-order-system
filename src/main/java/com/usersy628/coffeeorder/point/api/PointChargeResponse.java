package com.usersy628.coffeeorder.point.api;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.usersy628.coffeeorder.point.application.PointChargeResult;

public record PointChargeResponse(
	long userId,
	long chargedAmount,
	long balance,
	OffsetDateTime chargedAt
) {

	private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

	public static PointChargeResponse from(PointChargeResult result) {
		return new PointChargeResponse(
			result.userId(),
			result.chargedAmount(),
			result.balance(),
			result.chargedAt().atZone(KOREA_ZONE).toOffsetDateTime()
		);
	}
}
