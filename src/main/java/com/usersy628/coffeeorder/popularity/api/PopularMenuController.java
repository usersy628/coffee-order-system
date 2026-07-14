package com.usersy628.coffeeorder.popularity.api;

import com.usersy628.coffeeorder.popularity.application.PopularMenuQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/menus")
public class PopularMenuController {

	private final PopularMenuQueryService popularMenuQueryService;

	public PopularMenuController(PopularMenuQueryService popularMenuQueryService) {
		this.popularMenuQueryService = popularMenuQueryService;
	}

	@GetMapping("/popular")
	public PopularMenuResponse getPopularMenus() {
		return PopularMenuResponse.from(popularMenuQueryService.getPopularMenus());
	}
}
