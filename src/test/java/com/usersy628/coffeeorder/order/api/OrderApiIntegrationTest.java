package com.usersy628.coffeeorder.order.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.usersy628.coffeeorder.support.testcontainers.MySqlIntegrationTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@MySqlIntegrationTest
class OrderApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void resetOrderData() {
		jdbcTemplate.update("DELETE FROM order_event_outbox");
		jdbcTemplate.update("DELETE FROM point_history");
		jdbcTemplate.update("DELETE FROM order_item");
		jdbcTemplate.update("DELETE FROM orders");
		jdbcTemplate.update("UPDATE point_wallet SET balance = 30000, updated_at = UTC_TIMESTAMP(6)");
	}

	@Test
	void createsAnOrderWithMultipleMenuItems() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/users/1/orders")
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
			.andExpect(jsonPath("$.totalAmount").value(14000))
			.andExpect(jsonPath("$.balanceAfter").value(16000))
			.andExpect(jsonPath("$.items[0].menuId").value(1))
			.andExpect(jsonPath("$.items[1].menuId").value(2))
			.andReturn();

		Assertions.assertThat(count("orders")).isEqualTo(1);
		Assertions.assertThat(count("order_item")).isEqualTo(2);
		Assertions.assertThat(count("point_history")).isEqualTo(1);
		Assertions.assertThat(count("order_event_outbox")).isEqualTo(1);
		Assertions.assertThat(jdbcTemplate.queryForObject(
			"SELECT status FROM order_event_outbox", String.class)).isEqualTo("PENDING");
		Assertions.assertThat(jdbcTemplate.queryForObject(
			"SELECT JSON_EXTRACT(payload, '$.data.items[0].menuId') FROM order_event_outbox", Long.class
		)).isEqualTo(1L);

		Timestamp orderCreatedAt = jdbcTemplate.queryForObject(
			"SELECT created_at FROM orders", Timestamp.class);
		Timestamp orderPaidAt = jdbcTemplate.queryForObject(
			"SELECT paid_at FROM orders", Timestamp.class);
		Timestamp walletUpdatedAt = jdbcTemplate.queryForObject(
			"SELECT updated_at FROM point_wallet WHERE user_id = 1", Timestamp.class);
		Timestamp historyCreatedAt = jdbcTemplate.queryForObject(
			"SELECT created_at FROM point_history", Timestamp.class);
		Timestamp outboxCreatedAt = jdbcTemplate.queryForObject(
			"SELECT created_at FROM order_event_outbox", Timestamp.class);
		String occurredAt = jdbcTemplate.queryForObject(
			"SELECT JSON_UNQUOTE(JSON_EXTRACT(payload, '$.occurredAt')) FROM order_event_outbox", String.class);
		JsonNode responseBody = objectMapper.readTree(result.getResponse().getContentAsByteArray());

		Assertions.assertThat(orderCreatedAt).isEqualTo(orderPaidAt);
		Assertions.assertThat(walletUpdatedAt).isEqualTo(orderCreatedAt);
		Assertions.assertThat(historyCreatedAt).isEqualTo(orderCreatedAt);
		Assertions.assertThat(outboxCreatedAt).isEqualTo(orderCreatedAt);
		Assertions.assertThat(java.time.Instant.parse(occurredAt)).isEqualTo(orderCreatedAt.toInstant());
		Assertions.assertThat(OffsetDateTime.parse(responseBody.get("paidAt").asText()).toInstant())
			.isEqualTo(orderCreatedAt.toInstant());
		Assertions.assertThat(orderCreatedAt.getNanos() % 1_000).isZero();
	}

	@Test
	void rejectsAnUnknownUserWithoutPartialWrites() throws Exception {
		mockMvc.perform(post("/api/users/999/orders")
				.header("Idempotency-Key", "unknown-user-order")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"items\":[{\"menuId\":1,\"quantity\":1}]}"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));

		Assertions.assertThat(count("orders")).isZero();
		Assertions.assertThat(count("point_history")).isZero();
		Assertions.assertThat(count("order_event_outbox")).isZero();
	}

	@Test
	void replaysTheSameNormalizedRequestWithoutChargingAgain() throws Exception {
		MvcResult first = performOrder("replay-order", """
			{"items":[{"menuId":2,"quantity":1},{"menuId":1,"quantity":2}]}
			""").andExpect(status().isCreated())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
				.string("Idempotency-Replayed", "false"))
			.andReturn();
		MvcResult replay = performOrder("replay-order", """
			{"items":[{"menuId":1,"quantity":2},{"menuId":2,"quantity":1}]}
			""").andExpect(status().isOk())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
				.string("Idempotency-Replayed", "true"))
			.andReturn();

		JsonNode firstBody = objectMapper.readTree(first.getResponse().getContentAsByteArray());
		JsonNode replayBody = objectMapper.readTree(replay.getResponse().getContentAsByteArray());
		Assertions.assertThat(replayBody).isEqualTo(firstBody);
		Assertions.assertThat(count("orders")).isEqualTo(1);
		Assertions.assertThat(walletBalance()).isEqualTo(16_000L);
	}

	@Test
	void rejectsReusingTheSameKeyForDifferentItems() throws Exception {
		performOrder("reused-order", "{\"items\":[{\"menuId\":1,\"quantity\":1}]}")
			.andExpect(status().isCreated());

		performOrder("reused-order", "{\"items\":[{\"menuId\":1,\"quantity\":2}]}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));

		Assertions.assertThat(count("orders")).isEqualTo(1);
		Assertions.assertThat(walletBalance()).isEqualTo(25_500L);
	}

	@Test
	void rejectsUnavailableMenuAndInsufficientBalanceWithoutPartialWrites() throws Exception {
		performOrder("stopped-menu", "{\"items\":[{\"menuId\":3,\"quantity\":1}]}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MENU_NOT_ON_SALE"));
		performOrder("missing-menu", "{\"items\":[{\"menuId\":999,\"quantity\":1}]}")
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MENU_NOT_FOUND"));
		jdbcTemplate.update("UPDATE point_wallet SET balance = 1000 WHERE user_id = 1");
		performOrder("insufficient", "{\"items\":[{\"menuId\":1,\"quantity\":1}]}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INSUFFICIENT_POINTS"));

		Assertions.assertThat(count("orders")).isZero();
		Assertions.assertThat(count("point_history")).isZero();
		Assertions.assertThat(count("order_event_outbox")).isZero();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"{\"items\":[]}",
		"{\"items\":[null]}",
		"{\"items\":[{\"quantity\":1}]}",
		"{\"items\":[{\"menuId\":1}]}",
		"{\"items\":[{\"menuId\":1,\"quantity\":0}]}",
		"{\"items\":[{\"menuId\":1,\"quantity\":-1}]}",
		"{\"items\":[{\"menuId\":1,\"quantity\":2147483648}]}",
		"{\"items\":[{\"menuId\":1,\"quantity\":1},{\"menuId\":1,\"quantity\":2}]}"
	})
	void rejectsInvalidOrderItems(String body) throws Exception {
		performOrder("invalid-order", body)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_ORDER_REQUEST"));
	}

	private org.springframework.test.web.servlet.ResultActions performOrder(String key, String body) throws Exception {
		return mockMvc.perform(post("/api/users/1/orders")
			.header("Idempotency-Key", key)
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private int count(String table) {
		Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
		return count == null ? 0 : count;
	}

	private long walletBalance() {
		Long balance = jdbcTemplate.queryForObject(
			"SELECT balance FROM point_wallet WHERE user_id = 1", Long.class);
		return balance == null ? 0 : balance;
	}
}
