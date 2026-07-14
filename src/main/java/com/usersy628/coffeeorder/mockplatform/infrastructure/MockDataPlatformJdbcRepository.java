package com.usersy628.coffeeorder.mockplatform.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MockDataPlatformJdbcRepository {

	private final JdbcTemplate jdbcTemplate;

	public MockDataPlatformJdbcRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void saveIfAbsent(String eventId, String payload) {
		jdbcTemplate.update("""
			INSERT INTO mock_data_platform_received_event (event_id, payload, received_at)
			VALUES (?, ?, UTC_TIMESTAMP(6))
			ON DUPLICATE KEY UPDATE event_id = event_id
			""", eventId, payload);
	}
}
