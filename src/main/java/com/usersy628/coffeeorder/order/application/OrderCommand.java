package com.usersy628.coffeeorder.order.application;

import java.util.List;

public record OrderCommand(long userId, List<Item> items, String idempotencyKey) {

	public OrderCommand {
		items = List.copyOf(items);
	}

	public record Item(long menuId, int quantity) {
	}
}
