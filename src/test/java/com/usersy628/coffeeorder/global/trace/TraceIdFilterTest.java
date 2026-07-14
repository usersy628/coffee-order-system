package com.usersy628.coffeeorder.global.trace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class TraceIdFilterTest {

	private final TraceIdFilter traceIdFilter = new TraceIdFilter();
	private final ObjectMapper objectMapper = new ObjectMapper();
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		MDC.clear();
		mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
			.addFilters(traceIdFilter)
			.build();
	}

	@AfterEach
	void tearDown() {
		MDC.clear();
	}

	@Test
	void generatesAServerTraceIdAndIgnoresTheIncomingHeader() throws Exception {
		MvcResult result = mockMvc.perform(get("/test/success")
				.header(TraceIdFilter.TRACE_ID_HEADER, "client-controlled-trace-id"))
			.andExpect(status().isOk())
			.andReturn();

		String headerTraceId = result.getResponse().getHeader(TraceIdFilter.TRACE_ID_HEADER);
		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());

		assertThat(headerTraceId)
			.matches("[0-9a-f]{32}")
			.isNotEqualTo("client-controlled-trace-id");
		assertThat(body.path("traceId").asText()).isEqualTo(headerTraceId);
		assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
	}

	@Test
	void clearsMdcWhenTheFilterChainThrows() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		assertThatThrownBy(() -> traceIdFilter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
			assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).matches("[0-9a-f]{32}");
			throw new ServletException("test failure");
		})).isInstanceOf(ServletException.class);

		assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
	}

	@RestController
	static class TestController {

		@GetMapping("/test/success")
		Map<String, String> success() {
			return Map.of("traceId", MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY));
		}
	}
}
