package com.usersy628.coffeeorder.order.application;

import java.time.Instant;
import java.util.List;

public record OrderResult(
	long orderId,
	long userId,
	String status,
	long totalAmount,
	long balanceAfter,
	List<Item> items,
	Instant paidAt,
	boolean replayed
) {
	public record Item(long menuId, String menuName, long unitPrice, int quantity, long lineAmount) {
	}
}
