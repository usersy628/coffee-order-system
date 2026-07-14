package com.usersy628.coffeeorder.order.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.usersy628.coffeeorder.support.testcontainers.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@MySqlIntegrationTest
class OrderApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void createsAnOrderWithMultipleMenuItems() throws Exception {
		mockMvc.perform(post("/api/users/1/orders")
				.header("Idempotency-Key", "order-red-test-key")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "items": [
					    {"menuId": 2, "quantity": 1},
					    {"menuId": 1, "quantity": 2}
					  ]
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.orderId").isNumber())
			.andExpect(jsonPath("$.items[0].menuId").value(1))
			.andExpect(jsonPath("$.items[1].menuId").value(2));
	}
}
