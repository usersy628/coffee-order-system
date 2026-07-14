package com.usersy628.coffeeorder.menu.api;

import com.usersy628.coffeeorder.menu.domain.Menu;
import com.usersy628.coffeeorder.menu.domain.MenuStatus;

public record MenuResponse(
	Long menuId,
	String name,
	long price,
	MenuStatus status
) {

	public static MenuResponse from(Menu menu) {
		return new MenuResponse(menu.getId(), menu.getName(), menu.getPrice(), menu.getStatus());
	}
}
