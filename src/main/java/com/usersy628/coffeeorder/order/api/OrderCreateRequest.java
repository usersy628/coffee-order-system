package com.usersy628.coffeeorder.order.api;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderCreateRequest(
	@NotEmpty List<@NotNull @Valid Item> items
) {

	@AssertTrue
	public boolean isMenuIdUnique() {
		if (items == null) {
			return true;
		}
		Set<Long> menuIds = new HashSet<>();
		return items.stream()
			.filter(item -> item != null && item.menuId() != null)
			.allMatch(item -> menuIds.add(item.menuId()));
	}

	public record Item(
		@NotNull @Positive Long menuId,
		@NotNull @Positive @Max(Integer.MAX_VALUE) Long quantity
	) {
	}
}
