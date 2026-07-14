package com.usersy628.coffeeorder.point.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PointChargeRequest(
	@NotNull
	@Min(1)
	@Max(300_000)
	Long amount
) {
}
