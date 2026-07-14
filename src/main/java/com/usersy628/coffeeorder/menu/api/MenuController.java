package com.usersy628.coffeeorder.menu.api;

import java.util.List;

import com.usersy628.coffeeorder.menu.application.MenuQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/menus")
public class MenuController {

	private final MenuQueryService menuQueryService;

	public MenuController(MenuQueryService menuQueryService) {
		this.menuQueryService = menuQueryService;
	}

	@GetMapping
	public List<MenuResponse> getMenus() {
		return menuQueryService.getMenus().stream()
			.map(MenuResponse::from)
			.toList();
	}
}
