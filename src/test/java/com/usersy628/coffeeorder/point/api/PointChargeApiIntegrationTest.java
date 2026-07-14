package com.usersy628.coffeeorder.point.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.usersy628.coffeeorder.support.testcontainers.MySqlIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@MySqlIntegrationTest
class PointChargeApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void resetPointData() {
		jdbcTemplate.update("DELETE FROM point_history");
		jdbcTemplate.update("UPDATE point_wallet SET balance = 0, updated_at = UTC_TIMESTAMP(6)");
	}

	@AfterEach
	void restorePointData() {
		resetPointData();
	}

	@Test
	void chargesPointsAndReturnsTheUpdatedBalance() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/users/1/points/charges")
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
			.andExpect(jsonPath("$.chargedAt", matchesPattern(".+\\+09:00")))
			.andReturn();

		assertThat(walletBalance(1L)).isEqualTo(10_000L);
		assertThat(historyCount(1L)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("""
			SELECT CHAR_LENGTH(request_hash)
			FROM point_history
			WHERE user_id = 1 AND idempotency_key = 'charge-red-test-key'
			""", Integer.class)).isEqualTo(64);
		String storedCreatedAtText = jdbcTemplate.queryForObject("""
			SELECT DATE_FORMAT(created_at, '%Y-%m-%dT%H:%i:%s.%fZ')
			FROM point_history
			WHERE user_id = 1 AND idempotency_key = 'charge-red-test-key'
			""", String.class);
		String responseChargedAt = objectMapper.readTree(result.getResponse().getContentAsByteArray())
			.path("chargedAt")
			.asText();
		assertThat(storedCreatedAtText).isNotNull();
		Instant storedCreatedAt = Instant.parse(storedCreatedAtText);
		assertThat(storedCreatedAt.getNano() % 1_000).isZero();
		assertThat(OffsetDateTime.parse(responseChargedAt).toInstant()).isEqualTo(storedCreatedAt);
	}

	@Test
	void acceptsTheExactMaximumBalance() throws Exception {
		performCharge(1L, "maximum-balance", 300_000L)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.balance").value(300_000));

		assertThat(walletBalance(1L)).isEqualTo(300_000L);
		assertThat(historyCount(1L)).isEqualTo(1);
	}

	@Test
	void rejectsAChargeThatWouldExceedTheTotalBalanceLimitAtomically() throws Exception {
		jdbcTemplate.update("UPDATE point_wallet SET balance = 299999 WHERE user_id = 1");

		performCharge(1L, "over-total-limit", 2L)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("POINT_LIMIT_EXCEEDED"))
			.andExpect(jsonPath("$.details").isEmpty());

		assertThat(walletBalance(1L)).isEqualTo(299_999L);
		assertThat(historyCount(1L)).isZero();
	}

	@ParameterizedTest
	@ValueSource(longs = {0L, -1L, 300_001L})
	void rejectsAnInvalidChargeAmountWithFieldDetails(long invalidAmount) throws Exception {
		performCharge(1L, "invalid-amount-" + invalidAmount, invalidAmount)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_CHARGE_AMOUNT"))
			.andExpect(jsonPath("$.details.fieldErrors[0].field").value("amount"))
			.andExpect(jsonPath("$.details.fieldErrors[0].rejectedValue").value(invalidAmount));

		assertThat(walletBalance(1L)).isZero();
		assertThat(historyCount(1L)).isZero();
	}

	@Test
	void rejectsABlankIdempotencyKey() throws Exception {
		performCharge(1L, "   ", 100L)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_IDEMPOTENCY_KEY"));
	}

	@Test
	void rejectsAnIdempotencyKeyLongerThanTwoHundredFiftyFiveCharacters() throws Exception {
		performCharge(1L, "k".repeat(256), 100L)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_IDEMPOTENCY_KEY"));
	}

	@Test
	void rejectsAMissingIdempotencyKey() throws Exception {
		mockMvc.perform(post("/api/users/1/points/charges")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\":100}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REQUIRED"));
	}

	@Test
	void rejectsAStringAmountAsMalformedJson() throws Exception {
		mockMvc.perform(post("/api/users/1/points/charges")
				.header("Idempotency-Key", "string-amount")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\":\"100\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
	}

	@Test
	void returnsUserNotFoundWithoutWritingHistory() throws Exception {
		performCharge(999L, "missing-user", 100L)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));

		assertThat(historyCount(999L)).isZero();
	}

	@Test
	void replaysTheFirstResponseForTheSameKeyAndAmount() throws Exception {
		MvcResult first = performCharge(1L, "same-request", 12_345L)
			.andExpect(status().isOk())
			.andExpect(header().string("Idempotency-Replayed", "false"))
			.andReturn();
		MvcResult replay = performCharge(1L, "same-request", 12_345L)
			.andExpect(status().isOk())
			.andExpect(header().string("Idempotency-Replayed", "true"))
			.andReturn();

		JsonNode firstBody = objectMapper.readTree(first.getResponse().getContentAsByteArray());
		JsonNode replayBody = objectMapper.readTree(replay.getResponse().getContentAsByteArray());
		assertThat(replayBody).isEqualTo(firstBody);
		assertThat(walletBalance(1L)).isEqualTo(12_345L);
		assertThat(historyCount(1L)).isEqualTo(1);
	}

	@Test
	void rejectsReusingTheSameKeyForADifferentAmount() throws Exception {
		performCharge(1L, "reused-key", 100L).andExpect(status().isOk());

		performCharge(1L, "reused-key", 200L)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));

		assertThat(walletBalance(1L)).isEqualTo(100L);
		assertThat(historyCount(1L)).isEqualTo(1);
	}

	@Test
	void treatsIdempotencyKeysAsCaseSensitive() throws Exception {
		performCharge(1L, "Case-Key", 100L).andExpect(status().isOk());
		performCharge(1L, "case-key", 200L).andExpect(status().isOk());

		assertThat(walletBalance(1L)).isEqualTo(300L);
		assertThat(historyCount(1L)).isEqualTo(2);
	}

	private org.springframework.test.web.servlet.ResultActions performCharge(
		long userId,
		String idempotencyKey,
		long amount
	) throws Exception {
		return mockMvc.perform(post("/api/users/{userId}/points/charges", userId)
			.header("Idempotency-Key", idempotencyKey)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"amount\":" + amount + "}"));
	}

	private long walletBalance(long userId) {
		Long balance = jdbcTemplate.queryForObject(
			"SELECT balance FROM point_wallet WHERE user_id = ?",
			Long.class,
			userId
		);
		return balance == null ? 0L : balance;
	}

	private int historyCount(long userId) {
		Integer count = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM point_history WHERE user_id = ?",
			Integer.class,
			userId
		);
		return count == null ? 0 : count;
	}
}
