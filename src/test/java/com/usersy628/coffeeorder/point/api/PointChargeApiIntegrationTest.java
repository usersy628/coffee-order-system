package com.usersy628.coffeeorder.point.api;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.usersy628.coffeeorder.support.testcontainers.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@MySqlIntegrationTest
@Transactional
class PointChargeApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void chargesPointsAndReturnsTheUpdatedBalance() throws Exception {
		mockMvc.perform(post("/api/users/1/points/charges")
				.header("Idempotency-Key", "charge-red-test-key")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"amount": 10000}
					"""))
			.andExpect(status().isOk())
			.andExpect(header().string("Idempotency-Replayed", "false"))
			.andExpect(header().string("X-Trace-Id", matchesPattern("[0-9a-f]{32}")))
			.andExpect(jsonPath("$.userId").value(1))
			.andExpect(jsonPath("$.chargedAmount").value(10000))
			.andExpect(jsonPath("$.balance").value(10000))
			.andExpect(jsonPath("$.chargedAt").isString());
	}
}
