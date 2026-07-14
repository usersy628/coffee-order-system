package com.usersy628.coffeeorder.popularity.api;

import com.usersy628.coffeeorder.popularity.application.PopularMenu;
import com.usersy628.coffeeorder.popularity.application.PopularMenuQueryService;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;

public record PopularMenuResponse(OffsetDateTime from, OffsetDateTime to, List<Item> items) {

	private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

	public static PopularMenuResponse from(PopularMenuQueryService.QueryResult result) {
		return new PopularMenuResponse(
			result.from().atZone(KOREA_ZONE).toOffsetDateTime(),
			result.to().atZone(KOREA_ZONE).toOffsetDateTime(),
			IntStream.range(0, result.items().size())
				.mapToObj(index -> Item.from(index + 1, result.items().get(index)))
				.toList()
		);
	}

	public record Item(int rank, long menuId, String menuName, long totalQuantity) {

		private static Item from(int rank, PopularMenu popularMenu) {
			return new Item(rank, popularMenu.menuId(), popularMenu.menuName(), popularMenu.totalQuantity());
		}
	}
}
