package com.usersy628.coffeeorder.mockplatform.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.usersy628.coffeeorder.mockplatform.application.MockDataPlatformService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"local", "test"})
@RequestMapping("/internal/mock-data-platform/events")
public class MockDataPlatformController {

	private final MockDataPlatformService mockDataPlatformService;
	private final ObjectMapper objectMapper;

	public MockDataPlatformController(MockDataPlatformService mockDataPlatformService, ObjectMapper objectMapper) {
		this.mockDataPlatformService = mockDataPlatformService;
		this.objectMapper = objectMapper;
	}

	@PostMapping
	public ResponseEntity<Void> receive(@RequestBody JsonNode payload) {
		JsonNode eventId = payload.get("eventId");
		if (eventId == null || !eventId.isTextual() || eventId.asText().isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		try {
			mockDataPlatformService.receive(eventId.asText(), objectMapper.writeValueAsString(payload));
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize mock data platform event", exception);
		}
		return ResponseEntity.ok().build();
	}
}
