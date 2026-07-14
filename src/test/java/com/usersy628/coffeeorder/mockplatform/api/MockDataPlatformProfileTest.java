package com.usersy628.coffeeorder.mockplatform.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usersy628.coffeeorder.mockplatform.application.MockDataPlatformService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MockDataPlatformProfileTest {

	@Test
	void exposesTheReceiverOnlyForLocalAndTestProfiles() {
		try (AnnotationConfigApplicationContext defaultContext = contextWith()) {
			assertThat(defaultContext.getBeansOfType(MockDataPlatformController.class)).isEmpty();
		}
		try (AnnotationConfigApplicationContext localContext = contextWith("local")) {
			assertThat(localContext.getBeansOfType(MockDataPlatformController.class)).hasSize(1);
		}
		try (AnnotationConfigApplicationContext testContext = contextWith("test")) {
			assertThat(testContext.getBeansOfType(MockDataPlatformController.class)).hasSize(1);
		}
	}

	private AnnotationConfigApplicationContext contextWith(String... profiles) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.getEnvironment().setActiveProfiles(profiles);
		context.registerBean(MockDataPlatformService.class, () -> mock(MockDataPlatformService.class));
		context.registerBean(ObjectMapper.class, () -> new ObjectMapper());
		context.register(MockDataPlatformController.class);
		context.refresh();
		return context;
	}
}
