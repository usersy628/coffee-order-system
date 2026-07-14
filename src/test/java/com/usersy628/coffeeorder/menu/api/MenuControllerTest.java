package com.usersy628.coffeeorder.menu.api;

import java.util.List;

import com.usersy628.coffeeorder.menu.application.MenuQueryService;
import com.usersy628.coffeeorder.menu.domain.Menu;
import com.usersy628.coffeeorder.menu.domain.MenuStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MenuController.class)
class MenuControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MenuQueryService menuQueryService;

	@Test
	void returnsTheMenuResponseContract() throws Exception {
		Menu stoppedMenu = menu(10L, "판매 중지 메뉴", 6000L, MenuStatus.STOPPED);
		Menu onSaleMenu = menu(20L, "카푸치노", 5200L, MenuStatus.ON_SALE);
		when(menuQueryService.getMenus()).thenReturn(List.of(stoppedMenu, onSaleMenu));

		mockMvc.perform(get("/api/menus"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(header().string("X-Trace-Id", matchesPattern("[0-9a-f]{32}")))
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].length()").value(4))
			.andExpect(jsonPath("$[0].menuId").value(10))
			.andExpect(jsonPath("$[0].name").value("판매 중지 메뉴"))
			.andExpect(jsonPath("$[0].price").value(6000))
			.andExpect(jsonPath("$[0].status").value("STOPPED"))
			.andExpect(jsonPath("$[1].menuId").value(20))
			.andExpect(jsonPath("$[1].status").value("ON_SALE"));
	}

	@Test
	void returnsAnEmptyArrayWhenTheServiceFindsNoMenus() throws Exception {
		when(menuQueryService.getMenus()).thenReturn(List.of());

		mockMvc.perform(get("/api/menus"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(header().string("X-Trace-Id", matchesPattern("[0-9a-f]{32}")))
			.andExpect(content().json("[]"));
	}

	private Menu menu(long id, String name, long price, MenuStatus status) {
		Menu menu = mock(Menu.class);
		when(menu.getId()).thenReturn(id);
		when(menu.getName()).thenReturn(name);
		when(menu.getPrice()).thenReturn(price);
		when(menu.getStatus()).thenReturn(status);
		return menu;
	}
}
