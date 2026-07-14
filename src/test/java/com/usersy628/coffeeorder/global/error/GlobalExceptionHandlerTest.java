package com.usersy628.coffeeorder.global.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.usersy628.coffeeorder.global.trace.TraceIdFilter;
import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@Import(GlobalExceptionHandlerTest.TestController.class)
class GlobalExceptionHandlerTest {

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MockMvc mockMvc;

	@AfterEach
	void tearDown() {
		MDC.clear();
	}

	@Test
	void mapsDomainExceptionToTheErrorCodeContract() throws Exception {
		MvcResult result = mockMvc.perform(get("/test/domain-error"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
			.andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
			.andExpect(jsonPath("$.details.userId").value(1))
			.andReturn();

		assertTraceIdMatchesHeader(result);
		assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
	}

	@Test
	void mapsMalformedJsonWithoutLeakingParserDetails() throws Exception {
		MvcResult result = mockMvc.perform(post("/test/json")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\":\"100\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
			.andExpect(jsonPath("$.details").isEmpty())
			.andReturn();

		assertTraceIdMatchesHeader(result);
		assertThat(result.getResponse().getContentAsString()).doesNotContain("\"100\"");
	}

	@Test
	void hidesUnexpectedExceptionInformation() throws Exception {
		MvcResult result = mockMvc.perform(get("/test/unexpected-error"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
			.andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()))
			.andExpect(jsonPath("$.details").isEmpty())
			.andReturn();

		assertTraceIdMatchesHeader(result);
		assertThat(result.getResponse().getContentAsString())
			.doesNotContain("non-sensitive-internal-marker")
			.doesNotContain(IllegalStateException.class.getName());
	}

	@Test
	void keepsAnUnknownEndpointAsNotFoundInsteadOfMisclassifyingItAsAServerError() throws Exception {
		MvcResult result = mockMvc.perform(get("/unknown-endpoint"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("ENDPOINT_NOT_FOUND"))
			.andExpect(jsonPath("$.details").isEmpty())
			.andReturn();

		assertTraceIdMatchesHeader(result);
	}

	@Test
	void mapsANonNumericUserIdToInvalidUserId() throws Exception {
		MvcResult result = mockMvc.perform(get("/test/users/not-a-number"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_USER_ID"))
			.andExpect(jsonPath("$.details").isEmpty())
			.andReturn();

		assertTraceIdMatchesHeader(result);
	}

	@Test
	void mapsAUserIdBelowOneToInvalidUserId() throws Exception {
		MvcResult result = mockMvc.perform(get("/test/users/0"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_USER_ID"))
			.andExpect(jsonPath("$.details").isEmpty())
			.andReturn();

		assertTraceIdMatchesHeader(result);
	}

	@Test
	void mapsAMissingIdempotencyKeyToTheRequiredHeaderContract() throws Exception {
		MvcResult result = mockMvc.perform(post("/test/idempotent-json")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\":100}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REQUIRED"))
			.andExpect(jsonPath("$.details").isEmpty())
			.andReturn();

		assertTraceIdMatchesHeader(result);
	}

	@Test
	void mapsAnUnsupportedContentTypeWithoutLeakingFrameworkDetails() throws Exception {
		MvcResult result = mockMvc.perform(post("/test/idempotent-json")
				.header("Idempotency-Key", "test-key")
				.contentType(MediaType.TEXT_PLAIN)
				.content("amount=100"))
			.andExpect(status().isUnsupportedMediaType())
			.andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
			.andExpect(jsonPath("$.details").isEmpty())
			.andReturn();

		assertTraceIdMatchesHeader(result);
		assertThat(result.getResponse().getContentAsString()).doesNotContain("text/plain");
	}

	private void assertTraceIdMatchesHeader(MvcResult result) throws Exception {
		String headerTraceId = result.getResponse().getHeader(TraceIdFilter.TRACE_ID_HEADER);
		JsonNode responseBody = objectMapper.readTree(result.getResponse().getContentAsByteArray());

		assertThat(headerTraceId).matches("[0-9a-f]{32}");
		assertThat(responseBody.path("traceId").asText()).isEqualTo(headerTraceId);
	}

	@RestController
	@RequestMapping("/test")
	static class TestController {

		@GetMapping("/domain-error")
		void domainError() {
			throw new DomainException(ErrorCode.USER_NOT_FOUND, Map.of("userId", 1L));
		}

		@GetMapping("/unexpected-error")
		void unexpectedError() {
			throw new IllegalStateException("non-sensitive-internal-marker");
		}

		@PostMapping("/json")
		Map<String, Long> json(@RequestBody TestRequest request) {
			return Map.of("amount", request.amount());
		}

		@GetMapping("/users/{userId}")
		Map<String, Long> user(@PathVariable @Min(1) long userId) {
			return Map.of("userId", userId);
		}

		@PostMapping(value = "/idempotent-json", consumes = MediaType.APPLICATION_JSON_VALUE)
		Map<String, Long> idempotentJson(
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@RequestBody TestRequest request
		) {
			return Map.of("amount", request.amount());
		}
	}

	private record TestRequest(long amount) {
	}
}
