package com.usersy628.coffeeorder.mockplatform.application;

import com.usersy628.coffeeorder.mockplatform.infrastructure.MockDataPlatformJdbcRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MockDataPlatformService {

	private final MockDataPlatformJdbcRepository repository;

	public MockDataPlatformService(MockDataPlatformJdbcRepository repository) {
		this.repository = repository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void receive(String eventId, String payload) {
		repository.saveIfAbsent(eventId, payload);
	}
}
