package com.usersy628.coffeeorder.order.api;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import com.usersy628.coffeeorder.order.application.OrderResult;

public record OrderCreateResponse(
	long orderId,
	long userId,
	String status,
	long totalAmount,
	long balanceAfter,
	List<Item> items,
	OffsetDateTime paidAt
) {
	private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

	public static OrderCreateResponse from(OrderResult result) {
		return new OrderCreateResponse(
			result.orderId(), result.userId(), result.status(), result.totalAmount(), result.balanceAfter(),
			result.items().stream().map(Item::from).toList(),
			result.paidAt().atZone(KOREA_ZONE).toOffsetDateTime()
		);
	}

	public record Item(long menuId, String menuName, long unitPrice, int quantity, long lineAmount) {
		private static Item from(OrderResult.Item item) {
			return new Item(item.menuId(), item.menuName(), item.unitPrice(), item.quantity(), item.lineAmount());
		}
	}
}
